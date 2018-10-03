package zrz.webports.netty.sni;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLException;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import zrz.webports.spi.SniProvider;

public class SelfSignedSniMapper implements SniProvider {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SelfSignedSniMapper.class);
  private final Map<String, SslContext> contexts = new HashMap<>();

  public SelfSignedSniMapper() {

  }

  @Override
  public Flowable<SslContext> map(String input) {

    if (input == null) {
      // default ...
      input = "unknown.domain";
    }

    final SslContext sslCtx = this.contexts.computeIfAbsent(input, _name -> {
      try {

        final Set<String> ciphers = new HashSet<>();

        ciphers.addAll(Http2SecurityUtil.CIPHERS);
        ciphers.add("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384");
        ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
        ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");

        final SelfSignedCertificate cert = new SelfSignedCertificate();

        return SslContextBuilder
            .forServer(cert.key(), cert.cert())
            .ciphers(ciphers, new DebuggingCipherSuiteFilter(SupportedCipherSuiteFilter.INSTANCE))
            .applicationProtocolConfig(
                new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1))
            .build();

      }
      catch (final SSLException | CertificateException e) {
        throw new RuntimeException(e);
      }
    }

    );

    return Flowable.just(sslCtx);

  }

  // ELB supported ciphers:
  // Cipher Suite: TLS_RSA_WITH_AES_256_GCM_SHA384 (0x009d)
  // Cipher Suite: TLS_RSA_WITH_AES_256_CBC_SHA256 (0x003d)
  // Cipher Suite: TLS_RSA_WITH_AES_256_CBC_SHA (0x0035)
  // Cipher Suite: TLS_RSA_WITH_CAMELLIA_256_CBC_SHA (0x0084)
  // Cipher Suite: TLS_RSA_WITH_AES_128_GCM_SHA256 (0x009c)
  // Cipher Suite: TLS_RSA_WITH_AES_128_CBC_SHA256 (0x003c)
  // Cipher Suite: TLS_RSA_WITH_AES_128_CBC_SHA (0x002f)
  // Cipher Suite: TLS_RSA_WITH_CAMELLIA_128_CBC_SHA (0x0041)
  // Cipher Suite: TLS_RSA_WITH_RC4_128_SHA (0x0005)
  // Cipher Suite: TLS_RSA_WITH_3DES_EDE_CBC_SHA (0x000a)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 (0x00a3)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 (0x009f)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 (0x006b)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 (0x006a)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_256_CBC_SHA (0x0039)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_256_CBC_SHA (0x0038)
  // Cipher Suite: TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA (0x0088)
  // Cipher Suite: TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA (0x0087)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 (0x00a2)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 (0x009e)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 (0x0067)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_128_CBC_SHA256 (0x0040)
  // Cipher Suite: TLS_DHE_RSA_WITH_AES_128_CBC_SHA (0x0033)
  // Cipher Suite: TLS_DHE_DSS_WITH_AES_128_CBC_SHA (0x0032)
  // Cipher Suite: TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA (0x0045)
  // Cipher Suite: TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA (0x0044)
  // Cipher Suite: TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA (0x0016)
  // Cipher Suite: TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA (0x0013)
  // Cipher Suite: TLS_EMPTY_RENEGOTIATION_INFO_SCSV (0x00ff)

}
