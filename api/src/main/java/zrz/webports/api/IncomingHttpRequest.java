package zrz.webports.api;

import io.netty.handler.codec.http.HttpContent;
import io.reactivex.Flowable;

/**
 * an incoming HTTP/1.1 request, not sent over a h2 transport.
 *
 * @author theo
 *
 */

public interface IncomingHttpRequest extends WebPortHttpRequest {

  /**
   * the headers in the request.
   */

  WebPortHttpHeaders headers();

  /**
   * the HTTP content (if any), which will be one or more chunks until the chunk is a
   * LastHttpContennt.
   */

  Flowable<HttpContent> incoming();

}
