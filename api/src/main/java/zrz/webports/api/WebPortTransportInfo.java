package zrz.webports.api;

import java.net.SocketAddress;
import java.security.Principal;

/**
 * API that provides information about the underlying transport for a WebPort request.
 * 
 * @author theo
 *
 */

public interface WebPortTransportInfo {

  /**
   * true if the local incoming socket was secured.
   */

  boolean isSecure();

  /**
   * the local address/port
   */

  SocketAddress localAddress();

  /**
   * the remote address
   */

  SocketAddress remoteAddress();

  /**
   * the ALPN protocol
   */

  String applicationProtocol();

  /**
   * the protocol used. this will be HTTP/1.1 or H2.
   */

  String protocol();

  /**
   * Returns an X500Principal of the end-entity certificate for X509-based cipher suites. If no principal was sent, then
   * null is returned.
   */

  Principal localPrincipal();

  /**
   * SNI name from the ClientHello, if TLS.
   */

  String localServerName();

}
