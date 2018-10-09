package zrz.webports.netty.http11;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.Flowable;
import zrz.webports.WebPortContext;
import zrz.webports.netty.NettyHttpTransportInfo;
import zrz.webports.spi.HttpTransportInfo;
import zrz.webports.spi.IncomingHttpRequest;

public class PlainHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlainHttpHandler.class);
  private final WebPortContext ctx;

  public PlainHttpHandler(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest req) throws Exception {

    final NettyHttpTransportInfo transport = NettyHttpTransportInfo.fromChannel(ctx.channel());

    final Flowable<HttpObject> res = this.ctx.http(
        new IncomingHttpRequest() {

          @Override
          public HttpHeaders headers() {
            return req.headers();
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
          public HttpTransportInfo transport() {
            return transport;
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
        },
        () -> {
          ctx.close();
        });

  }

}
