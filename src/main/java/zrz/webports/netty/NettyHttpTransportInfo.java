package zrz.webports.netty;

import java.net.SocketAddress;
import java.security.Principal;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SniCompletionEvent;
import io.netty.handler.ssl.SslHandler;
import zrz.webports.spi.HttpTransportInfo;

public class NettyHttpTransportInfo implements HttpTransportInfo {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NettyHttpTransportInfo.class);

  private String applicationProtocol;
  private final boolean isEncrypted;
  private String protocol;
  private String cipherSuite;

  private Principal localPrincipal;

  private final SocketAddress remote;

  private final SocketAddress local;

  private final SniCompletionEvent sni;

  public NettyHttpTransportInfo(final Channel channel) {

    final SslHandler ssl = channel.pipeline().get(SslHandler.class);

    this.sni = channel.attr(HttpServerConnector.SNI).get();

    this.isEncrypted = ssl != null;

    this.remote = (channel.remoteAddress());
    this.local = (channel.localAddress());

    if (this.isEncrypted) {
      this.applicationProtocol = ssl.applicationProtocol();
      final SSLEngine engine = ssl.engine();
      final SSLSession session = engine.getSession();
      this.protocol = session.getProtocol();
      this.cipherSuite = session.getCipherSuite();
      this.localPrincipal = session.getLocalPrincipal();
    }
  }

  public static NettyHttpTransportInfo fromChannel(final Channel channel) {

    return new NettyHttpTransportInfo(channel);

  }

  @Override
  public String toString() {

    if (!this.isEncrypted) {
      return "PLAIN " + this.remote + " -> " + this.local;
    }

    return "TLS " + this.remote + " -> " + this.local + " " +
        this.applicationProtocol + " " +
        this.protocol + " " +
        this.cipherSuite + " " +
        this.localPrincipal + " " +
        this.sni;

  }

  @Override
  public boolean isSecure() {
    return this.isEncrypted;
  }

  @Override
  public String applicationProtocol() {
    return this.applicationProtocol;
  }

  @Override
  public String protocol() {
    return this.protocol;
  }

  @Override
  public Principal localPrincipal() {
    return this.localPrincipal;
  }

  @Override
  public String localServerName() {
    if (this.sni == null) {
      return null;
    }
    else if (!this.sni.isSuccess()) {
      return null;
    }
    return this.sni.hostname();
  }

  @Override
  public SocketAddress localAddress() {
    return this.local;
  }

  @Override
  public SocketAddress remoteAddress() {
    return this.remote;
  }

}
