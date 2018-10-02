package zrz.webports.netty.http11;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.Flowable;
import zrz.webports.WebPortContext;
import zrz.webports.spi.IncomingHttpRequest;

public class PlainHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private final WebPortContext ctx;

  public PlainHttpHandler(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest req) throws Exception {

    final Flowable<HttpObject> res = this.ctx.http(new IncomingHttpRequest() {

    });

    res.subscribe(
        msg -> {
          ctx.writeAndFlush(msg);
        },
        err -> {
          ctx.close();
        },
        () -> {
          ctx.close();
        });

  }

}
