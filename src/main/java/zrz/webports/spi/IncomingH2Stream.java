package zrz.webports.spi;

import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;

/**
 * the client opened a new h2 stream.
 *
 * @author theo
 *
 */

public interface IncomingH2Stream {

  /**
   * an incoming h2 stream always starts with headers. this contains the header that initiated it.
   */

  Http2Headers headers();

  /**
   * true if there was only a single header frame.
   */

  boolean isEndStream();

  /**
   *
   * @return
   */

  Flowable<Http2StreamFrame> incoming();

  HttpTransportInfo transport();

}
