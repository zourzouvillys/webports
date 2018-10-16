package zrz.webports.spi;

import java.net.SocketAddress;
import java.security.Principal;

public interface HttpTransportInfo {

  boolean isSecure();

  SocketAddress localAddress();

  SocketAddress remoteAddress();

  String applicationProtocol();

  String protocol();

  Principal localPrincipal();

  String localServerName();

}
