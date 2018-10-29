package zrz.webports.core.http;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.EmptyHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;
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

  /**
   * converts a h2 stream to a netty emulated http/1.1 one that deals with HttpObjects.
   *
   * @param object
   * @return
   */

  public static HttpObject toHttp(final Http2StreamFrame frame) {

    try {

      final boolean validateHeaders = true;

      if (frame instanceof Http2HeadersFrame) {

        final Http2HeadersFrame headersFrame = (Http2HeadersFrame) frame;
        final Http2Headers headers = headersFrame.headers();
        final Http2FrameStream stream = headersFrame.stream();
        final int id = stream == null ? 0 : stream.id();

        final CharSequence status = headers.status();

        // 100-continue response is a special case where Http2HeadersFrame#isEndStream=false
        // but we need to decode it as a FullHttpResponse to play nice with HttpObjectAggregator.

        if ((null != status) && HttpResponseStatus.CONTINUE.codeAsText().contentEquals(status)) {
          return HttpConversionUtil.toFullHttpResponse(id, headers, UnpooledByteBufAllocator.DEFAULT, validateHeaders);
        }

        if (headersFrame.isEndStream()) {

          if ((headers.method() == null) && (status == null)) {
            final LastHttpContent last = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, validateHeaders);
            HttpConversionUtil.addHttp2ToHttpHeaders(id, headers, last.trailingHeaders(), HttpVersion.HTTP_1_1, true, true);
            return last;
          }
          else {
            return HttpConversionUtil.toFullHttpResponse(id, headers, UnpooledByteBufAllocator.DEFAULT, validateHeaders);
          }
        }
        else {
          final HttpMessage req = HttpConversionUtil.toHttpResponse(id, headers, validateHeaders);
          if (!HttpUtil.isContentLengthSet(req)) {
            req.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
          }
          return req;
        }
      }
      else if (frame instanceof Http2DataFrame) {
        final Http2DataFrame dataFrame = (Http2DataFrame) frame;
        if (dataFrame.isEndStream()) {
          return new DefaultLastHttpContent(dataFrame.content().retain(), validateHeaders);
        }
        else {
          return new DefaultHttpContent(dataFrame.content().retain());
        }
      }

    }
    catch (final Http2Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new IllegalArgumentException(frame.toString());

  }

  public static Flowable<Http2StreamFrame> toHttp2(final HttpObject msg) {

    boolean endStream = false;
    final boolean validateHeaders = false;

    final ArrayList<Http2StreamFrame> out = new ArrayList<>();

    if (msg instanceof HttpMessage) {
      final HttpMessage httpMsg = (HttpMessage) msg;
      final Http2Headers http2Headers = HttpConversionUtil.toHttp2Headers(httpMsg, validateHeaders);
      endStream = (msg instanceof FullHttpMessage) && !((FullHttpMessage) msg).content().isReadable();
      out.add(new DefaultHttp2HeadersFrame(http2Headers, endStream));
    }

    if (!endStream && (msg instanceof HttpContent)) {

      boolean isLastContent = false;

      HttpHeaders trailers = EmptyHttpHeaders.INSTANCE;
      Http2Headers http2Trailers = EmptyHttp2Headers.INSTANCE;

      if (msg instanceof LastHttpContent) {
        isLastContent = true;
        // Convert any trailing headers.
        final LastHttpContent lastContent = (LastHttpContent) msg;
        trailers = lastContent.trailingHeaders();
        http2Trailers = HttpConversionUtil.toHttp2Headers(trailers, validateHeaders);
      }

      // Write the data
      final ByteBuf content = ((HttpContent) msg).content();

      endStream = isLastContent && trailers.isEmpty();

      out.add(new DefaultHttp2DataFrame(content, endStream));

      if (!trailers.isEmpty()) {
        out.add(new DefaultHttp2HeadersFrame(http2Trailers, true));
      }

    }
    else {

      out.add(new DefaultHttp2DataFrame(true));

    }

    return Flowable.fromIterable(out);

  }

}
