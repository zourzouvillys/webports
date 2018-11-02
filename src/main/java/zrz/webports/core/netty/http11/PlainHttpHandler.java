package zrz.webports.core.netty.http11;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingHttpRequest;
import zrz.webports.api.WebPortHttpHeaders;
import zrz.webports.api.WebPortTransportInfo;
import zrz.webports.core.WebPortContext;
import zrz.webports.core.netty.NettyHttpTransportInfo;

public class PlainHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlainHttpHandler.class);
  private final WebPortContext ctx;

  public PlainHttpHandler(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest req) throws Exception {

    final NettyHttpTransportInfo transport = NettyHttpTransportInfo.fromChannel(ctx.channel());

    ByteBuf content;

    if (req instanceof FullHttpRequest) {
      content = ((FullHttpRequest) req).content().retain();
    }
    else {
      content = null;
    }

    final Flowable<HttpObject> res =
      this.ctx.http(
        new IncomingHttpRequest() {

          WrappedHttp1Headers wrappedHeaders = new WrappedHttp1Headers(req.headers());

          @Override
          public WebPortHttpHeaders headers() {
            return this.wrappedHeaders;
          }

          @Override
          public Flowable<HttpContent> incoming() {
            final FullHttpRequest full = (FullHttpRequest) req;
            return Flowable.just(full);
          }

          @Override
          public CharSequence method() {
            return req.method().asciiName();
          }

          @Override
          public String path() {
            return req.uri();
          }

          @Override
          public WebPortTransportInfo transport() {
            return transport;
          }

          @Override
          public CharSequence scheme() {
            return this.transport().isSecure() ? "https"
                                               : "http";
          }

        });

    res.subscribe(
      msg -> {
        log.debug("sending {}", msg);
        ctx.writeAndFlush(msg);
      },
      err -> {
        log.warn("error on transmission stream: {}", err.getMessage(), err);
        ctx.close();
        if (content != null) {
          content.release();
        }
      },
      () -> {
        ctx.close();
        if (content != null) {
          content.release();
        }
      });

  }

}
