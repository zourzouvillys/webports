package zrz.webports.api;

import java.util.function.Function;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;

/**
 * the client opened a new h2 stream.
 *
 * @author theo
 *
 */

public interface IncomingH2Stream extends WebPortHttpRequest {

  /**
   * an incoming h2 stream always starts with headers. this contains the headers that initiated it.
   */

  WebPortHttpHeaders headers();

  /**
   * true if there was only a single header frame and this is the end of the stream.
   */

  boolean isEndStream();

  /**
   * the stream which will contain future frames.
   */

  Flowable<Http2StreamFrame> incoming();

  /**
   * maps the incoming stream and the response to HTTP/1.1 API semantics, allowing a single handler
   * to be used for both HTTP/1.1 and H2.
   *
   * @param httpHandler
   * @return
   */

  Flowable<Http2StreamFrame> toHttp(Function<IncomingHttpRequest, Flowable<HttpObject>> httpHandler);

}
