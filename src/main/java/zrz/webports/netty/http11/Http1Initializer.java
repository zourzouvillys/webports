package zrz.webports.netty.http11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
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
import io.netty.util.AsciiString;
import zrz.webports.WebPortContext;

public class Http1Initializer extends ChannelInitializer<Channel> implements UpgradeCodecFactory {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Http1Initializer.class);
  private final WebPortContext ctx;

  public Http1Initializer(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void initChannel(final Channel ch) throws Exception {

    final ChannelPipeline p = ch.pipeline();

    final HttpServerCodec sourceCodec = new HttpServerCodec();

    p.addLast(sourceCodec);

    // for now, we force a single object.
    p.addLast(new HttpObjectAggregator(65535, true));

    // allow http1.1 upgrades to h2 and to websockets.
    p.addLast(new HttpServerUpgradeHandler(sourceCodec, this));

    // otherwise fall back to plain http/1.1.
    // TODO: remove this when upgrading to h2/ws
    p.addLast(Http1Initializer.this.createRawHttp1Handler());

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
