package zrz.webports.core;

import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingH2Stream;
import zrz.webports.api.IncomingHttpRequest;
import zrz.webports.api.IncomingWebSocket;
import zrz.webports.core.netty.h2.IngressHttp2StreamHandler;
import zrz.webports.spi.SniProvider;

public class DefaultWebPortContext implements WebPortContext {

  private final SniProvider sni;
  private final Function<IncomingH2Stream, Flowable<Http2StreamFrame>> h2;
  private final Function<IncomingWebSocket, Flowable<WebSocketFrame>> ws;
  private final Function<IncomingHttpRequest, Flowable<HttpObject>> http;

  public DefaultWebPortContext(
      final Supplier<Function<IncomingH2Stream, Flowable<Http2StreamFrame>>> h2,

      final Supplier<Function<IncomingWebSocket, Flowable<WebSocketFrame>>> ws,
      final Supplier<Function<IncomingHttpRequest, Flowable<HttpObject>>> http,
      final SniProvider mapper) {
    // this.sni = new LocalSniMapper();
    this.sni = mapper;
    this.h2 = h2.get();
    this.ws = ws.get();
    this.http = http.get();
  }

  @Override
  public SniProvider sni() {
    return this.sni;
  }

  @Override
  public Http2MultiplexCodec h2Handler() {
    return Http2MultiplexCodecBuilder
      .forServer(new IngressHttp2StreamHandler(this, this.h2))
      // .validateHeaders(true)
      .initialSettings(Http2Settings
        .defaultSettings()
        .initialWindowSize(1024 * 128)
        .maxConcurrentStreams(1024))
      .frameLogger(new Http2FrameLogger(LogLevel.TRACE))
      .build();
  }

  @Override
  public Flowable<WebSocketFrame> websocket(final IncomingWebSocket incoming) {
    return this.ws.apply(incoming);
  }

  @Override
  public Flowable<HttpObject> http(final IncomingHttpRequest incoming) {
    return this.http.apply(incoming);
  }

}
