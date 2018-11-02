package zrz.webports.core.netty.h2;

import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingH2Stream;
import zrz.webports.api.IncomingHttpRequest;
import zrz.webports.api.WebPortHttpHeaders;
import zrz.webports.api.WebPortTransportInfo;

public class Http2ToHttp1RequestWrapper implements IncomingHttpRequest {

  private final IncomingH2Stream h2;

  public Http2ToHttp1RequestWrapper(final IncomingH2Stream h2) {
    this.h2 = h2;
  }

  @Override
  public CharSequence method() {
    return this.h2.method();
  }

  @Override
  public CharSequence path() {
    return this.h2.path();
  }

  @Override
  public CharSequence scheme() {
    return this.h2.scheme();
  }

  @Override
  public WebPortTransportInfo transport() {
    return this.h2.transport();
  }

  @Override
  public WebPortHttpHeaders headers() {
    return this.h2.headers();
  }

  @Override
  public Flowable<HttpContent> incoming() {
    return this.h2.incoming()
      .filter(frame -> frame instanceof Http2DataFrame)
      .cast(Http2DataFrame.class)
      .map(frame -> new DefaultHttpContent(frame.content()));
  }

}
