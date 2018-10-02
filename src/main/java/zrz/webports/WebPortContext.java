package zrz.webports;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsyncMapping;
import io.reactivex.Flowable;
import zrz.webports.spi.IncomingHttpRequest;
import zrz.webports.spi.IncomingWebSocket;

public interface WebPortContext {

  AsyncMapping<? super String, ? extends SslContext> sni();

  Http2MultiplexCodec h2Handler();

  Flowable<WebSocketFrame> websocket(IncomingWebSocket incoming);

  Flowable<HttpObject> http(IncomingHttpRequest incoming);

}
