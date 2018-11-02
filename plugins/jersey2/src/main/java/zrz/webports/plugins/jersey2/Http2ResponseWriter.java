package zrz.webports.plugins.jersey2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.reactivestreams.Subscriber;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import zrz.webports.api.IncomingH2Stream;

class Http2ResponseWriter extends Flowable<Http2StreamFrame>
    implements ContainerResponseWriter, Runnable {

  private final UnicastProcessor<Http2StreamFrame> sink;
  private ByteBufOutputStream content;
  private ByteBuf buffer;

  public Http2ResponseWriter(final IncomingH2Stream req) {
    this.sink = UnicastProcessor.create(16, this, false);
  }

  @Override
  public OutputStream writeResponseStatusAndHeaders(
      final long contentLength,
      final ContainerResponse res)
      throws ContainerException {

    final Http2Headers headers = new DefaultHttp2Headers();

    headers.status(Integer.toString(res.getStatus()));

    for (final Entry<String, List<String>> e : res.getStringHeaders().entrySet()) {
      headers.add(e.getKey().toLowerCase(), e.getValue());
    }

    final boolean hasBody = contentLength != 0;

    headers.setLong(HttpHeaderNames.CONTENT_LENGTH, contentLength);

    this.sink.onNext(new DefaultHttp2HeadersFrame(headers, !hasBody));

    this.buffer = Unpooled.buffer();
    this.content = new ByteBufOutputStream(this.buffer);

    return this.content;
  }

  @Override
  public boolean suspend(
      final long timeOut,
      final TimeUnit timeUnit,
      final TimeoutHandler timeoutHandler) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented Method: ContainerResponseWriter.suspend invoked.");
  }

  @Override
  public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit)
      throws IllegalStateException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented Method: ContainerResponseWriter.setSuspendTimeout invoked.");
  }

  @Override
  public void commit() {

    try {
      this.content.flush();
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }

    this.sink.onNext(new DefaultHttp2DataFrame(this.buffer, true));
    this.sink.onComplete();
  }

  @Override
  public void failure(final Throwable error) {
    // we need to send a 500 ourselves.
    final Http2Headers headers = new DefaultHttp2Headers();
    headers.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText());
    this.sink.onNext(new DefaultHttp2HeadersFrame(headers, true));
  }

  @Override
  public boolean enableResponseBuffering() {
    return true;
  }

  @Override
  protected void subscribeActual(final Subscriber<? super Http2StreamFrame> s) {
    this.sink.subscribe(s);
  }

  @Override
  public void run() {
    this.sink.onComplete();
  }
}
