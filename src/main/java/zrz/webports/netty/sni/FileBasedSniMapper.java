package zrz.webports.netty.sni;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLException;

import com.google.common.net.InternetDomainName;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.reactivex.Flowable;
import zrz.webports.spi.SniProvider;

public class FileBasedSniMapper implements SniProvider {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileBasedSniMapper.class);
  private final Map<String, SslContext> contexts = new HashMap<>();
  private final Map<InternetDomainName, Path> wildcards = new HashMap<>();
  private final Map<InternetDomainName, Path> paths = new HashMap<>();

  public FileBasedSniMapper(final String envname) {

    final String key = System.getenv(envname);

    if (key == null) {
      return;
    }

    final Path certdir = Paths.get(key);

    try {

      if (!Files.isDirectory(certdir)) {
        return;
      }

      Files.list(certdir)
          .forEach(p -> {

            if (!Files.isDirectory(p)) {
              return;
            }

            final String name = p.getFileName().toString();

            if (name.startsWith("_.") && InternetDomainName.isValid(name.substring(2))) {
              final InternetDomainName idn = InternetDomainName.from(p.getFileName().toString().substring(2));
              log.info("adding wildcard for {}", p.getFileName());
              this.wildcards.put(idn, p.toAbsolutePath());
            }
            else if (InternetDomainName.isValid(name)) {
              final InternetDomainName idn = InternetDomainName.from(p.getFileName().toString());
              log.info("adding cert for {}", idn);
              this.paths.put(idn, p.toAbsolutePath());
            }
            else {
              log.warn("invalid domain name for cert: {}", name);
            }

          });

    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public Flowable<SslContext> map(String input) {

    if (input == null) {
      // default ...
      input = this.wildcards.keySet().iterator().next().toString();
    }

    // TODO: cache

    return Flowable.just(this.generate(input));

  }

  SslContext generate(final String _name) {

    try {

      final Set<String> ciphers = new HashSet<>();

      ciphers.addAll(Http2SecurityUtil.CIPHERS);

      ciphers.add("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384");
      ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
      ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");

      final InternetDomainName idn = InternetDomainName.from(_name);

      Path certdir = this.paths.containsKey(idn)
          ? this.paths.get(idn)
          : this.wildcards.get(idn.parent());

      if (certdir == null) {
        certdir = this.wildcards.values().iterator().next();
      }

      log.debug("using {} for {}", certdir, idn);

      return SslContextBuilder
          .forServer(
              certdir.resolve("tls.crt").toFile(),
              certdir.resolve("tls.key").toFile())
          .ciphers(ciphers, SupportedCipherSuiteFilter.INSTANCE)
          .protocols("TLSv1.3", "TLSv1.2")
          // .ciphers(ciphers, new DebuggingCipherSuiteFilter(SupportedCipherSuiteFilter.INSTANCE))
          .applicationProtocolConfig(
              new ApplicationProtocolConfig(
                  ApplicationProtocolConfig.Protocol.ALPN,
                  ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                  ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                  ApplicationProtocolNames.HTTP_2,
                  ApplicationProtocolNames.HTTP_1_1))
          .build();
    }
    catch (final SSLException e) {
      throw new RuntimeException(e);
    }

  }

}
