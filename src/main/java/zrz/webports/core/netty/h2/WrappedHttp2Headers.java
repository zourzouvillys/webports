package zrz.webports.core.netty.h2;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.handler.codec.http2.Http2Headers;
import zrz.webports.api.WebPortHttpHeaders;

public class WrappedHttp2Headers implements WebPortHttpHeaders {

  private final Http2Headers headers;

  public WrappedHttp2Headers(final Http2Headers headers) {
    this.headers = headers;
  }

  @Override
  public Iterator<Entry<CharSequence, CharSequence>> iterator() {
    return this.headers.iterator();
  }

  @Override
  public CharSequence get(final CharSequence headerName) {
    return this.headers.get(headerName);
  }

  @Override
  public Iterable<@NonNull ? extends CharSequence> getAll(final CharSequence headerName) {
    return this.headers.getAll(headerName);
  }

}
