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

  String path();

  /**
   * List of the requested WS protocols.
   */

  default String[] protocols() {
    return null;
  }

  /**
   * List of supported WS extensions.
   */

  default String[] extensions() {
    return null;
  }

  /**
   * the requested authority (e.h, Host header in http/1.1, :authority in h2).
   */

  String authority();

  /**
   * The Origin header from the request.
   */

  String origin();

  /**
   * authorization headers.
   */

  Iterable<String> authorizations();

  /**
   * the incoming frames, if accepted.
   */

  Flowable<WebSocketFrame> incoming();

}
