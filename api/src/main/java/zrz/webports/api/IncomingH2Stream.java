package zrz.webports.api;

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
   * an incoming h2 stream always starts with headers. this contains the headers that initiated it.
   */

  Http2Headers headers();

  /**
   * true if there was only a single header frame.
   */

  boolean isEndStream();

  /**
   * the stream which will contain future frames.
   */

  Flowable<Http2StreamFrame> incoming();

  /**
   * info about the transport this request was stream was opened on.
   */

  WebPortTransportInfo transport();

}
