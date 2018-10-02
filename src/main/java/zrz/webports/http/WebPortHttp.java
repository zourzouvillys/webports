package zrz.webports.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;

public class WebPortHttp {

  public static Flowable<HttpObject> staticResponse(final int code) {
    return Flowable.just(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code), Unpooled.EMPTY_BUFFER));
  }

  public static Flowable<HttpObject> staticResponse(final int code, final String reason) {
    return Flowable.just(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(code, reason), Unpooled.EMPTY_BUFFER));
  }

  public static Flowable<Http2StreamFrame> staticResponseH2(final int code, final String reason) {

    final Http2Headers headers = new DefaultHttp2Headers()
        .status(new HttpResponseStatus(code, reason).codeAsText());

    return Flowable.just(new DefaultHttp2HeadersFrame(headers, true));

  }

}
