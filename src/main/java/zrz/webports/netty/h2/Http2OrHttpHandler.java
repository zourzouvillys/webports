package zrz.webports.netty.h2;

import javax.net.ssl.SSLHandshakeException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import zrz.webports.WebPortContext;
import zrz.webports.netty.HttpUtils;

/**
 * Used during protocol negotiation (connection establishment), adds either a http/1.1 or h2 handler to the end of the
 * pipeline.
 */

public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Http2OrHttpHandler.class);
  private final WebPortContext ctx;

  public Http2OrHttpHandler(final WebPortContext ctx) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.ctx = ctx;
  }

  @Override
  protected void handshakeFailure(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

    if (cause instanceof SSLHandshakeException) {

      // final SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
      // final OpenSslEngine engine = (OpenSslEngine) sslHandler.engine();
      log.info("SSL handshake failure: {}", cause.getMessage());
      // Arrays.toString(engine.getSSLParameters().getCipherSuites()));

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
    ctx.pipeline().addLast(this.ctx.h2Handler());
  }

  /**
   * it's http/1.1 - although the first message may be an upgrade to h2.
   *
   * @param ctx
   * @throws Exception
   */

  private void configureHttp1(final ChannelHandlerContext ctx) throws Exception {
    HttpUtils.configureHttp11(this.ctx, ctx.channel());
  }

}
