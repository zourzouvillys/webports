package zrz.webports.spi;

import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;

public interface IncomingH2Stream {

  Flowable<Http2StreamFrame> incoming();

}
