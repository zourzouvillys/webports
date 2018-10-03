package zrz.webports.netty.http11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import zrz.webports.WebPortContext;
import zrz.webports.netty.HttpUtils;

public class Http1Initializer extends ChannelInitializer<Channel> {

  private final WebPortContext ctx;

  public Http1Initializer(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void initChannel(final Channel ch) throws Exception {

    HttpUtils.configureHttp11(this.ctx, ch);

  }

  /**
   *
   */

  public Http2MultiplexCodec createH2Handler() {
    return this.ctx.h2Handler();
  }

  public PlainHttpHandler createRawHttp1Handler() {
    return new PlainHttpHandler(this.ctx);
  }

}
