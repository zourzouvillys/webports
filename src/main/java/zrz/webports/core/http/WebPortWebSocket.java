package zrz.webports.core.http;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.Flowable;

public class WebPortWebSocket {

  public static Flowable<WebSocketFrame> just(final String text) {
    return Flowable.just(new TextWebSocketFrame(text));
  }

}
