package zrz.webports.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;
import zrz.webports.WebPortContext;
import zrz.webports.netty.http11.PlainHttpHandler;
import zrz.webports.netty.http11.WebsocketUpgradeCodec;

public class HttpUtils {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HttpUtils.class);

  private static class CtxUpgradeCodecFactory implements UpgradeCodecFactory {

    private final WebPortContext ctx;

    public CtxUpgradeCodecFactory(final WebPortContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public UpgradeCodec newUpgradeCodec(final CharSequence protocol) {
      if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return new Http2ServerUpgradeCodec(this.ctx.h2Handler());
      }
      else if (AsciiString.contentEquals(HttpHeaderValues.WEBSOCKET, protocol)) {
        return new WebsocketUpgradeCodec(this.ctx);
      }
      log.info("unknown protocol for upgrade: {}", protocol);
      return null;
    }

  }

  public static void configureHttp11(final WebPortContext ctx, final Channel ch) {

    final ChannelPipeline p = ch.pipeline();

    final HttpServerCodec sourceCodec = new HttpServerCodec();

    p.addLast(sourceCodec);

    // allow http1.1 upgrades to h2. only ever the first request on a connection.
    p.addLast(new HttpServerUpgradeHandler(sourceCodec, new CtxUpgradeCodecFactory(ctx)));

    p.addLast("httpagg", new HttpObjectAggregator(65535, true));

    // otherwise fall back to plain http/1.1.
    p.addLast("http1handler", new PlainHttpHandler(ctx));

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

    // final ChannelPipeline p = ch.pipeline();
    //
    // final HttpServerCodec sourceCodec = new HttpServerCodec();
    //
    // p.addLast(sourceCodec);
    //
    // // allow http1.1 upgrades to h2 and to websockets.
    // p.addLast(new HttpServerUpgradeHandler(sourceCodec, this));
    //
    // // for now, we force a single object.
    // p.addLast(new HttpObjectAggregator(65535, true));
    //
    // // otherwise fall back to plain http/1.1.
    // // TODO: remove this when upgrading to h2/ws
    // p.addLast("http1handler", Http1Initializer.this.createRawHttp1Handler());

  }

}
