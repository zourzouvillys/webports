package zrz.webports.acme;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Identifier;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.TlsAlpn01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeRetryAfterException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.reactivex.Flowable;
import zrz.webports.spi.SniProvider;

public class AcmeService implements SniProvider {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AcmeService.class);
  private SslContext binding;
  private final KeyPair keyPair;
  private final String domains;
  private final String serverUri;

  public static final String DEFAULT_LETSENCRYPT_URI = "acme://letsencrypt.org/";

  public AcmeService(final String domain) {
    this(domain, DEFAULT_LETSENCRYPT_URI);
  }

  public AcmeService(final String domain, final String serverUri) {
    this.domains = domain;
    this.keyPair = loadPrivateKeys();
    this.serverUri = serverUri;
  }

  public void start() {

    try {

      Security.addProvider(new BouncyCastleProvider());

      final File certPath = new File(System.getProperty("user.home"), ".letsencrypt.cert");

      if (certPath.exists()) {
        throw new IllegalArgumentException();
      }

      final Session session = new Session(this.serverUri);

      // final Metadata meta = session.getMetadata();

      final KeyPair accountKeyPair = loadKeys();

      final Account account = this.loadAccount(session, accountKeyPair);

      ///

      // final Authorization auth = account.preAuthorizeDomain("adrp.app");

      // processAuth(auth);
      // System.exit(0);

      // Account account = login.getAccount();

      //

      final Order order = account.newOrder()
          .domains(this.domains)
          // .notAfter(Instant.now().plus(Duration.ofDays(20L)))
          .create();

      for (final Authorization auth : order.getAuthorizations()) {
        if (auth.getStatus() != Status.VALID) {
          this.processAuth(order.getIdentifiers(), auth);
        }
      }

      // authorized. get cert.

      //

      final CSRBuilder csrb = new CSRBuilder();
      csrb.addDomain(this.domains);
      csrb.sign(this.keyPair);
      csrb.write(new FileWriter(new File(System.getProperty("user.home"), ".letsencrypt.csr")));

      final byte[] csr = csrb.getEncoded();

      //

      order.execute(csr);

      //

      while (order.getStatus() != Status.VALID) {
        try {
          Thread.sleep(1000L);
          order.update();
        }
        catch (final AcmeRetryAfterException ex) {
          final long wait = Duration.between(Instant.now(), ex.getRetryAfter()).toMillis();
          log.info("sleeping {}", wait);
          Thread.sleep(wait);
        }
      }

      //

      final Certificate certificate = order.getCertificate();

      log.info("Got new certificate for {} ", certificate.getLocation());

      log.info("expires at {}", certificate.getCertificate().getNotAfter());

      // also write out so we can use again if restarted.

      try (final Writer writer = new BufferedWriter(new FileWriter(certPath))) {
        certificate.writeCertificate(writer);
      }

      this.bind(this.keyPair, certificate.getCertificateChain());

    }
    catch (final Exception ex) {

      ex.printStackTrace();

    }

  }

  private Account loadAccount(final Session session, final KeyPair accountKeyPair) throws AcmeException, IOException {

    final File accountInfo = new File(System.getProperty("user.home"), ".letsencrypt.account");

    if (accountInfo.exists()) {

      final URL accountLocationUrl = new URL(new String(Files.readAllBytes(accountInfo.toPath())));

      log.info("using account {}", accountLocationUrl);

      final Login login = session.login(accountLocationUrl, accountKeyPair);

      return login.getAccount();

    }
    else {

      final Account account = new AccountBuilder()
          .agreeToTermsOfService()
          .useKeyPair(accountKeyPair)
          .create(session);

      final URL accountLocationUrl = account.getLocation();

      Files.write(accountInfo.toPath(), accountLocationUrl.toExternalForm().getBytes(UTF_8), CREATE_NEW);

      return account;

    }
  }

  private void processAuth(final List<Identifier> list, final Authorization auth) {

    final TlsAlpn01Challenge challenge = auth.findChallenge(TlsAlpn01Challenge.TYPE);

    // challenge.trigger();

    try {

      final X509Certificate cert = CertificateUtils.createTlsAlpn01Certificate(
          this.keyPair,
          list.get(0).getDomain(),
          challenge.getAcmeValidation());

      this.bind(this.keyPair, cert);

      // bind and expose ...

    }
    catch (final IOException e1) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e1);
    }

    // System.err.println(challenge.getAuthorization());
    // System.err.println(challenge.getToken());

    System.err.println(challenge.getError());
    System.err.println(challenge.getLocation());
    System.err.println(challenge.getStatus());
    System.err.println(challenge.getValidated());

    try {
      challenge.trigger();
    }
    catch (final AcmeException e1) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e1);
    }

    while (challenge.getStatus() != Status.VALID) {

      try {

        Thread.sleep(3000L);

        challenge.update();

        System.err.println(challenge.getError());
        System.err.println(challenge.getLocation());
        System.err.println(challenge.getStatus());
        System.err.println(challenge.getValidated());

      }
      catch (final InterruptedException | AcmeException e) {
        // TODO Auto-generated catch block
        throw new RuntimeException(e);
      }
    }

  }

  private void bind(final KeyPair keyPair, final X509Certificate cert) {
    this.bind(keyPair, Arrays.asList(cert));
  }

  private void bind(final KeyPair keyPair, final List<X509Certificate> cert) {

    final Set<String> ciphers = new HashSet<>();

    // ciphers.addAll(Http2SecurityUtil.CIPHERS);
    ciphers.add("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384");
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
    ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");

    ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
    /* openssl = ECDHE-RSA-AES256-GCM-SHA384 */
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    /* openssl = ECDHE-ECDSA-CHACHA20-POLY1305 */
    // ciphers.add("TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256");
    /* openssl = ECDHE-RSA-CHACHA20-POLY1305 */
    // ciphers.add("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256");
    /* openssl = ECDHE-ECDSA-AES128-GCM-SHA256 */
    ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");

    /* REQUIRED BY HTTP/2 SPEC */
    /* openssl = ECDHE-RSA-AES128-GCM-SHA256 */
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

    // GCM (Galois/Counter Mode) requires JDK 8.
    ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
    ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    // AES256 requires JCE unlimited strength jurisdiction policy files.
    ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
    // GCM (Galois/Counter Mode) requires JDK 8.
    ciphers.add("TLS_RSA_WITH_AES_128_GCM_SHA256");
    ciphers.add("TLS_RSA_WITH_AES_128_CBC_SHA");
    // AES256 requires JCE unlimited strength jurisdiction policy files.
    ciphers.add("TLS_RSA_WITH_AES_256_CBC_SHA");

    try {
      this.binding = SslContextBuilder
          .forServer(keyPair.getPrivate(), cert.toArray(new X509Certificate[0]))
          .ciphers(ciphers, SupportedCipherSuiteFilter.INSTANCE)
          .applicationProtocolConfig(
              new ApplicationProtocolConfig(
                  ApplicationProtocolConfig.Protocol.ALPN,
                  ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                  ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                  ApplicationProtocolNames.HTTP_2,
                  ApplicationProtocolNames.HTTP_1_1,
                  "acme-tls/1"))
          .build();
    }
    catch (final SSLException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

  }

  private static KeyPair loadKeys() {

    try {

      final File file = new File(System.getProperty("user.home"), ".letsencrypt");

      if (file.exists()) {

        return KeyPairUtils.readKeyPair(new FileReader(file));

      }

      System.err.println(" -> generating new keypair");

      final KeyPair accountKeyPair = KeyPairUtils.createECKeyPair("secp256r1");

      final PrintWriter w = new PrintWriter(file);

      KeyPairUtils.writeKeyPair(accountKeyPair, w);

      w.flush();

      w.close();

      return accountKeyPair;

    }
    catch (final IOException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

  }

  private static KeyPair loadPrivateKeys() {

    try {

      final File file = new File(System.getProperty("user.home"), ".letsencrypt.keys");

      if (file.exists()) {

        return KeyPairUtils.readKeyPair(new FileReader(file));

      }

      System.err.println(" -> generating local TLS keypair");

      final KeyPair accountKeyPair = KeyPairUtils.createKeyPair(2048);

      final PrintWriter w = new PrintWriter(file);

      KeyPairUtils.writeKeyPair(accountKeyPair, w);

      w.flush();

      w.close();

      return accountKeyPair;

    }
    catch (final IOException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

  }

  @Override
  public Flowable<SslContext> map(final String fqdn) {
    return Flowable.just(this.binding);
  }

}
