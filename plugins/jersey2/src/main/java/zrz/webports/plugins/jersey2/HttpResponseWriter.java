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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import zrz.webports.api.IncomingHttpRequest;

class HttpResponseWriter extends Flowable<HttpObject>
    implements ContainerResponseWriter, Runnable {

  private final UnicastProcessor<HttpObject> sink;
  private ByteBufOutputStream content;
  private ByteBuf buffer;

  public HttpResponseWriter(final IncomingHttpRequest req) {
    this.sink = UnicastProcessor.create(16, this, false);
  }

  @Override
  public OutputStream writeResponseStatusAndHeaders(
      final long contentLength,
      final ContainerResponse res)
      throws ContainerException {

    final DefaultHttpResponse wres = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(res.getStatus()));

    for (final Entry<String, List<String>> e : res.getStringHeaders().entrySet()) {
      wres.headers().add(e.getKey().toLowerCase(), e.getValue());
    }

    final boolean hasBody = contentLength != 0;

    wres.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);

    this.sink.onNext(wres);

    if (!hasBody) {
      return null;
    }

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

    if (this.content != null) {
      try {
        this.content.flush();
      }
      catch (final IOException e) {
        throw new RuntimeException(e);
      }
      this.sink.onNext(new DefaultHttpContent(this.buffer));
    }

    this.sink.onComplete();

  }

  @Override
  public void failure(final Throwable error) {
    // we need to send a 500 ourselves.
    this.sink.onNext(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
    this.sink.onComplete();
  }

  @Override
  public boolean enableResponseBuffering() {
    return true;
  }

  @Override
  protected void subscribeActual(final Subscriber<? super HttpObject> s) {
    this.sink.subscribe(s);
  }

  @Override
  public void run() {
    this.sink.onComplete();
  }

}
