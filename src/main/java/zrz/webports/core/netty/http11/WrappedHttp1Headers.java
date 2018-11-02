package zrz.webports.core.netty.http11;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.handler.codec.http.HttpHeaders;
import zrz.webports.api.WebPortHttpHeaders;

public class WrappedHttp1Headers implements WebPortHttpHeaders {

  private final HttpHeaders headers;

  public WrappedHttp1Headers(final HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public Iterator<Entry<CharSequence, CharSequence>> iterator() {
    return this.headers.iteratorCharSequence();
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
