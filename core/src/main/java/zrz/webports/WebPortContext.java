package zrz.webports;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingHttpRequest;
import zrz.webports.api.IncomingWebSocket;
import zrz.webports.spi.SniProvider;

public interface WebPortContext {

  SniProvider sni();

  Http2MultiplexCodec h2Handler();

  Flowable<WebSocketFrame> websocket(IncomingWebSocket incoming);

  Flowable<HttpObject> http(IncomingHttpRequest incoming);

}
