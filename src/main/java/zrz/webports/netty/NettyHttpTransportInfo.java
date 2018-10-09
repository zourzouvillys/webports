package zrz.webports.netty;

import java.net.SocketAddress;
import java.security.Principal;
import java.util.Arrays;

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
      log.info("valiues: {}", Arrays.asList(engine.getSession().getValueNames()));
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

}
