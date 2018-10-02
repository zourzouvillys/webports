package zrz.webports.netty.wss;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import zrz.webports.WebPortContext;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketFrameHandler.class);
  private final WebPortContext ctx;
  private final String selectedSubprotocol;
  private final UnicastProcessor<WebSocketFrame> rxqueue;
  private final Flowable<WebSocketFrame> txmit;

  public WebSocketFrameHandler(
      final WebPortContext ctx,
      final String selectedSubprotocol,
      final Flowable<WebSocketFrame> handler,
      final UnicastProcessor<WebSocketFrame> rxqueue) {
    this.ctx = ctx;
    this.selectedSubprotocol = selectedSubprotocol;
    this.rxqueue = rxqueue;
    this.txmit = handler;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {

    if (frame instanceof TextWebSocketFrame) {

      this.rxqueue.onNext(frame);

    }
    else if (frame instanceof PingWebSocketFrame) {

      ctx.channel().writeAndFlush(new PongWebSocketFrame());

    }
    else if (frame instanceof CloseWebSocketFrame) {

      ctx.channel().writeAndFlush(frame.retain()).addListener(ChannelFutureListener.CLOSE);

    }
    else {

      final String message = "unsupported frame type: " + frame.getClass().getName();
      throw new UnsupportedOperationException(message);

    }

  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

}
