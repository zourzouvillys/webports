package zrz.webports.spi;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.Flowable;

/**
 * an incoming HTTP/1.1 request, not sent over a h2 transport.
 *
 * @author theo
 *
 */

public interface IncomingHttpRequest {

  /**
   * the headers in the request.
   */

  HttpHeaders headers();

  /**
   * the HTTP contennt (if any), which will be one or more chunks until the chunk is a LastHttpContennt.
   */

  Flowable<HttpContent> incoming();

  CharSequence method();

  String path();

}
