package zrz.webports.spi;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.Flowable;

/**
 * an incoming websocket connection request that needs to be serviced.
 *
 * a websocket remains open as long as there is a subscriber on the incoming frames or the transmission flowable is not
 * completed.
 *
 * if the tranmission flow returns an error, the websocket will be closed immediately. if an error occurs on the
 * upstream, the flowable will complete with an error.
 *
 * for an orderly shutdown, the upstream flowable will complete.
 *
 * @author theo
 *
 */

public interface IncomingWebSocket {

  /**
   * The path of the websocket.
   */

  default String path() {
    return null;
  }

  /**
   * the name provided in the HTTP Host header.
   */

  default String host() {
    return null;
  }

  /**
   * List of the requested protocols.
   */

  default String[] protocols() {
    return null;
  }

  /**
   * List of supported extensions.
   */

  default String[] extensions() {
    return null;
  }

  /**
   * The Origin header from the request.
   */

  default String origin() {
    return null;
  }

  /**
   *
   * @return
   */

  Flowable<WebSocketFrame> incoming();

}
