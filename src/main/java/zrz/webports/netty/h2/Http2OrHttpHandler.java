package zrz.webports.netty.h2;

import java.util.Arrays;

import javax.net.ssl.SSLHandshakeException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.OpenSslEngine;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import zrz.webports.WebPortContext;
import zrz.webports.netty.http11.PlainHttpHandler;
import zrz.webports.netty.http11.WebsocketUpgradeCodec;

/**
 * Used during protocol negotiation (connection establishment), adds either a http/1.1 or h2 handler to the end of the
 * pipeline.
 */

public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler implements UpgradeCodecFactory {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Http2OrHttpHandler.class);
  private final WebPortContext ctx;

  public Http2OrHttpHandler(final WebPortContext ctx) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.ctx = ctx;
  }

  @Override
  protected void handshakeFailure(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

    if (cause instanceof SSLHandshakeException) {

      final SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
      final OpenSslEngine engine = (OpenSslEngine) sslHandler.engine();

      log.info("SSL handshake failure, {}  {} ciphers {} protocol {}",
          Arrays.toString(engine.getSSLParameters().getCipherSuites()));

    }

    super.handshakeFailure(ctx, cause);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
  }

  /**
   *
   */

  @Override
  protected void configurePipeline(final ChannelHandlerContext ctx, final String protocol) throws Exception {

    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      this.configureHttp2(ctx);
      return;
    }

    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      this.configureHttp1(ctx);
      return;
    }

    throw new IllegalStateException("unknown protocol: " + protocol);

  }

  /**
   * a new h2 transport.
   */

  private void configureHttp2(final ChannelHandlerContext ctx) {
    ctx.pipeline().addLast(this.createH2Handler());
  }

  /**
   *
   */

  public Http2MultiplexCodec createH2Handler() {
    return this.ctx.h2Handler();
  }

  /**
   *
   */

  public PlainHttpHandler createRawHttp1Handler() {
    return new PlainHttpHandler(this.ctx);
  }

  /**
   * it's http/1.1 - although the first message may be an upgrade to h2.
   *
   * @param ctx
   * @throws Exception
   */

  private void configureHttp1(final ChannelHandlerContext ctx) throws Exception {

    final ChannelPipeline p = ctx.pipeline();

    final HttpServerCodec sourceCodec = new HttpServerCodec();

    p.addLast(sourceCodec);
    p.addLast(new HttpObjectAggregator(65535, true));

    // allow http1.1 upgrades to h2. only ever the first request on a connection.
    p.addLast(new HttpServerUpgradeHandler(sourceCodec, this.createUpgradeFactory()));

    // otherwise fall back to plain http/1.1.
    p.addLast(Http2OrHttpHandler.this.createRawHttp1Handler());

    // new SimpleChannelInboundHandler<HttpMessage>() {
    //
    // @Override
    // protected void channelRead0(final ChannelHandlerContext ctx, final HttpMessage msg) throws Exception {
    //
    // // it's not a h2 upgrade, so we treat as normal HTTP.
    // ctx.pipeline().replace(this, null, Http2OrHttpHandler.this.createRawHttp1Handler());
    //
    // // and dispatch into it.
    // ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
    //
    // }

    // });

  }

  private UpgradeCodecFactory createUpgradeFactory() {
    return this;
  }

  @Override
  public UpgradeCodec newUpgradeCodec(final CharSequence protocol) {
    if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
      return new Http2ServerUpgradeCodec(this.createH2Handler());
    }
    else if (AsciiString.contentEquals(HttpHeaderValues.WEBSOCKET, protocol)) {
      return new WebsocketUpgradeCodec(this.ctx);
    }
    log.info("unknown protocol for upgrade: {}", protocol);
    return null;
  }

}
