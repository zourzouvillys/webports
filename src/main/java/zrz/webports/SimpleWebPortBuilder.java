package zrz.webports;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.net.HostAndPort;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;
import zrz.webports.netty.NettyHttpListener;
import zrz.webports.netty.sni.SelfSignedSniMapper;
import zrz.webports.spi.IncomingH2Stream;
import zrz.webports.spi.IncomingHttpRequest;
import zrz.webports.spi.IncomingWebSocket;
import zrz.webports.spi.SniProvider;

/**
 * A basic webport builder useful for many common use cases.
 *
 * @author theo
 *
 */

public class SimpleWebPortBuilder {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleWebPortBuilder.class);

  private Flowable<HostAndPort> listen;
  private Supplier<Function<IncomingH2Stream, Flowable<Http2StreamFrame>>> h2;
  private Supplier<Function<IncomingWebSocket, Flowable<WebSocketFrame>>> ws;
  private Supplier<Function<IncomingHttpRequest, Flowable<HttpObject>>> http;

  private SniProvider mapper;

  public SimpleWebPortBuilder sni(final SniProvider mapper) {
    this.mapper = mapper;
    return this;
  }

  public SimpleWebPortBuilder listen(final int port) {
    this.listen(Flowable.just(Integer.valueOf(port)).map(e -> HostAndPort.fromParts("::0", port)).concatWith(Flowable.never()));
    return this;
  }

  public SimpleWebPortBuilder listen(final Flowable<HostAndPort> port) {
    this.listen = port;
    return this;
  }

  public SimpleWebPortBuilder h2(final Supplier<Function<IncomingH2Stream, Flowable<Http2StreamFrame>>> binder) {
    this.h2 = binder;
    return this;
  }

  public SimpleWebPortBuilder http(final Supplier<Function<IncomingHttpRequest, Flowable<HttpObject>>> binder) {
    this.http = binder;
    return this;
  }

  public SimpleWebPortBuilder websocket(final Supplier<Function<IncomingWebSocket, Flowable<WebSocketFrame>>> binder) {
    this.ws = binder;
    return this;
  }

  public WebPort build() {

    if (this.mapper == null) {
      this.mapper = new SelfSignedSniMapper();
    }

    final HostAndPort target = this.listen.blockingFirst();

    final DefaultWebPortContext context = new DefaultWebPortContext(this.h2, this.ws, this.http, this.mapper);

    final NettyHttpListener listener = new NettyHttpListener(target.getPort(), context);

    listener
        .startAsync()
        .awaitRunning();

    log.info("listening on {}", listener.listenHostAndPort());

    return new WebPort() {

      @Override
      public WebPort shutdown() {
        listener.stopAsync();
        return this;
      }

    };

  }

}
