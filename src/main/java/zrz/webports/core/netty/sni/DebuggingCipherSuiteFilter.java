package zrz.webports.core.netty.sni;

import java.util.List;
import java.util.Set;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

/**
 * dumps the cipher suite info to logging.
 *
 * @author theo
 *
 */

public class DebuggingCipherSuiteFilter implements CipherSuiteFilter {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DebuggingCipherSuiteFilter.class);
  private final SupportedCipherSuiteFilter target;

  public DebuggingCipherSuiteFilter(final SupportedCipherSuiteFilter instance) {
    this.target = instance;
  }

  @Override
  public String[] filterCipherSuites(final Iterable<String> ciphers, final List<String> defaultCiphers, final Set<String> supportedCiphers) {
    log.info("ciphers: {} (supported {})", ciphers, supportedCiphers);
    return this.target.filterCipherSuites(ciphers, defaultCiphers, supportedCiphers);
  }

}
