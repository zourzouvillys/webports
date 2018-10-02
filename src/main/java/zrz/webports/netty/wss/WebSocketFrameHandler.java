package zrz.webports.netty.wss;

import java.util.Locale;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketFrameHandler.class);
  private final WebSocketServerHandshaker handshaker;

  public WebSocketFrameHandler(final WebSocketServerHandshaker handshaker) {
    this.handshaker = handshaker;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {

    if (frame instanceof TextWebSocketFrame) {

      final String request = ((TextWebSocketFrame) frame).text();
      log.info("{} received {}", ctx.channel(), request);
      ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));

    }
    else if (frame instanceof CloseWebSocketFrame) {

      this.handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());

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
