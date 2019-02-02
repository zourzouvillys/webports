package zrz.webports.eventstream;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Duration;
import java.util.ArrayList;

import org.reactivestreams.Subscriber;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingHttpRequest;

public class EventStreamProcessor extends Flowable<HttpObject> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EventStreamProcessor.class);

  private Flowable<EventSource> source;
  private CharSequence origin;
  private Duration retry;

  public EventStreamProcessor(Duration retry, IncomingHttpRequest src, EventSourceProvider provider) {

    this.retry = retry;

    this.origin = src.headers().get(HttpHeaderNames.ORIGIN);

    CharSequence lastEventId = src.headers().get("last-event-id");

    log.debug("starting event stream with lastEventID={}, origin={}", lastEventId, origin);

    this.source =
      provider.create(src.path().toString(),
        lastEventId == null ? null
                            : lastEventId.toString());

  }

  @Override
  protected void subscribeActual(Subscriber<? super HttpObject> s) {

    ArrayList<HttpObject> initial = new ArrayList<>();

    DefaultHttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);

    if (origin != null) {
      res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
    }

    res.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");

    initial.add(res);

    if (this.retry != null) {
      ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
      buf.writeCharSequence("retry:" + this.retry.toMillis() + "\n\n", UTF_8);
      initial.add(new DefaultHttpContent(buf));
    }

    this.source
      .map(this::toBuffer)
      .startWith(initial)
      .subscribe(s);

  }

  private HttpObject toBuffer(EventSource e) {

    String id = e.id();
    String data = e.data();
    String event = e.event();

    if ((id == null) && (data == null) && (event == null)) {
      ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
      buf.writeCharSequence(":\n", UTF_8);
      return new DefaultHttpContent(buf);
    }

    StringBuilder sb = new StringBuilder();

    if (id != null) {
      sb.append("id:").append(e.id()).append("\n");
    }
    if (event != null) {
      sb.append("event:").append(e.event()).append("\n");
    }

    if (data != null) {
      data = data.replaceAll("\r?\n", "\ndata:");
      sb.append("data:").append(data).append("\n");
    }

    sb.append("\n");

    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
    buf.writeCharSequence(sb.toString(), UTF_8);
    return new DefaultHttpContent(buf);

  }

}
