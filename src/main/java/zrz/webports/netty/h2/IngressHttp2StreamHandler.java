package zrz.webports.netty.h2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.UnicastProcessor;
import zrz.webports.WebPortContext;
import zrz.webports.netty.NettyHttpTransportInfo;
import zrz.webports.spi.HttpTransportInfo;
import zrz.webports.spi.IncomingH2Stream;

/**
 * each incoming HTTP stream is passed to this handler. we map it to a flow to allow the application to handle it.
 */

@ChannelHandler.Sharable
public class IngressHttp2StreamHandler extends ChannelDuplexHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IngressHttp2StreamHandler.class);

  //
  private final WebPortContext ctx;
  private final Function<IncomingH2Stream, Flowable<Http2StreamFrame>> factory;

  private final static AttributeKey<ActiveStream> BINDER = AttributeKey.valueOf("H2-BINNDING");

  private static boolean isEndOfStream(final Http2StreamFrame frame) {
    switch (frame.name()) {
      case "DATA":
        return ((Http2DataFrame) frame).isEndStream();
      case "HEADERS":
        return ((Http2HeadersFrame) frame).isEndStream();
      case "WINDOW_UPDATE":
        return false;
      case "RESET":
        return true;
      default:
        log.error("unknown frame for stream: {}", frame.name());
        return true;
    }
  }

  private class ActiveStream implements IncomingH2Stream {

    // queue of received frames that the consumer has not yet consumed.
    private final UnicastProcessor<Http2StreamFrame> rxqueue = UnicastProcessor.create(16, this::cancelled, false);
    private final ChannelHandlerContext ctx;
    private final Flowable<Http2StreamFrame> txflow;
    private Disposable handle;
    private final AtomicBoolean willFlush = new AtomicBoolean(true);
    private final Http2HeadersFrame headers;
    private final NettyHttpTransportInfo transport;

    public ActiveStream(final ChannelHandlerContext ctx, final Http2HeadersFrame headers) {
      this.ctx = ctx;
      this.headers = headers;
      this.transport = NettyHttpTransportInfo.fromChannel(ctx.channel().parent());
      this.txflow = IngressHttp2StreamHandler.this.factory.apply(this);
    }

    void cancelled() {
      log.debug("H2 cancelled due to unnsubscribing from incoming frames");
    }

    public void accept(final Http2StreamFrame frame) {
      this.willFlush.set(true);
      this.rxqueue.onNext(frame);
      this.willFlush.set(false);
    }

    public void error(final Throwable cause) {
      cause.printStackTrace();
      this.rxqueue.onError(cause);
      this.close();
    }

    public void flush() {

      if (this.handle == null) {

        this.handle = this.txflow.subscribe(
            frame -> {

              if (!this.willFlush.get()) {
                this.ctx.writeAndFlush(frame);
              }
              else {
                this.ctx.write(frame);
              }

            },
            err -> {

              // an error causes an unnorderly shutdown of the stream.
              log.warn("H2 stream error caused by transmitting Flowable error: {}", err.getMessage(), err);
              this.ctx.writeAndFlush(new DefaultHttp2ResetFrame(Http2Error.INTERNAL_ERROR));

            },
            () -> {

              log.debug("H2 stream completed");

            });

      }

      this.willFlush.set(false);

    }

    void close() {
      if ((this.handle != null) && !this.handle.isDisposed()) {
        this.handle.dispose();
      }
    }

    @Override
    public Flowable<Http2StreamFrame> incoming() {
      return this.rxqueue.takeUntil((Predicate<Http2StreamFrame>) frame -> isEndOfStream(frame));
    }

    @Override
    public Http2Headers headers() {
      return this.headers.headers();
    }

    @Override
    public boolean isEndStream() {
      return this.headers.isEndStream();
    }

    @Override
    public HttpTransportInfo transport() {
      return this.transport;
    }

  }

  public IngressHttp2StreamHandler(final WebPortContext ctx, final Function<IncomingH2Stream, Flowable<Http2StreamFrame>> h2) {
    this.ctx = ctx;
    this.factory = h2;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

    if (msg instanceof Http2StreamFrame) {

      final Http2StreamFrame frame = (Http2StreamFrame) msg;

      final Attribute<ActiveStream> binding = ctx.channel().attr(BINDER);

      ActiveStream current = binding.get();

      if (binding == null) {
        // TODO: rejected by SPI, close stream.
      }

      if (current == null) {
        current = new ActiveStream(ctx, (Http2HeadersFrame) frame);
        binding.set(current);
      }
      else {

        current.accept(frame);

      }

    }
    else {

      log.warn("unexpected message received: {}", msg);
      super.channelRead(ctx, msg);

    }

  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

    cause.printStackTrace();

    log.error("exception {} on h2 stream: {}", cause.getClass().getName(), cause.getMessage(), cause);

    ctx.close();

    final ActiveStream binding = ctx.channel().attr(BINDER).get();

    if (binding != null) {
      binding.error(cause);
    }

  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {

    final ActiveStream binding = ctx.channel().attr(BINDER).get();

    if (binding != null) {
      binding.flush();
    }

    ctx.flush();
  }

}
