package zrz.webports.plugins.jersey2;

import java.net.URI;
import java.util.Map.Entry;

import org.glassfish.jersey.server.ContainerRequest;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;
import io.reactivex.Single;
import zrz.webports.api.IncomingH2Stream;

class Jersey2Http2Request extends AbstractJersey2Request {

  private final IncomingH2Stream req;

  Jersey2Http2Request(final IncomingH2Stream req, final WebPortJerseyDispatcher appHandler, final String base) {
    super(req.transport(), appHandler, base);
    this.req = req;
  }

  Flowable<Http2StreamFrame> start() {

    final ContainerRequest requestContext =
      new ContainerRequest(
        this.baseUri(),
        this.requestUri(),
        this.method(),
        this.securityContext(),
        this);

    for (final Entry<CharSequence, CharSequence> h : this.req.headers()) {
      requestContext.headers(h.getKey().toString(), h.getValue().toString());
    }

    final Http2ResponseWriter writer = new Http2ResponseWriter(this.req);

    requestContext.setWriter(writer);

    switch (this.req.method().toString()) {
      case "POST":
      case "PUT":
      case "PATCH": {
        final CharSequence cl = this.req.headers().get(HttpHeaderNames.CONTENT_LENGTH);

        if ((cl != null) && (Long.parseLong(cl.toString()) > 0)) {
          this.readFully()
            .subscribe(buf -> {
              requestContext.setEntityStream(new ByteBufInputStream(buf, true));
              this.appHandler.dispatch(requestContext);
            });
          return writer;
        }

        break;
      }
    }

    this.appHandler.dispatch(requestContext);

    return writer;
  }

  Single<CompositeByteBuf> readFully() {
    return this.req
      .incoming()
      .filter(Http2DataFrame.class::isInstance)
      .map(Http2DataFrame.class::cast)
      .filter(f -> f.content().readableBytes() > 0)
      .map(e -> e.content().retain())
      .reduce(
        Unpooled.compositeBuffer(),
        (buffer, frame) -> {
          buffer.addComponent(true, frame);
          return buffer;
        });
  }

  private URI baseUri() {
    return URI.create(this.base);
  }

  private URI requestUri() {
    final String path = this.req.path().toString();
    return URI.create(path);
  }

  private String method() {
    return this.req.method().toString();
  }

}
