package zrz.webports.netty;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import zrz.webports.WebPortContext;

public class HttpServerConnector extends ChannelInitializer<SocketChannel> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HttpServerConnector.class);

  private final WebPortContext ctx;
  private final UserEventLogger eventLogger;

  public HttpServerConnector(final WebPortContext ctx) {
    this.ctx = ctx;
    this.eventLogger = new UserEventLogger();
  }

  public ChannelInitializer<?> channelInitializer() {
    return this;
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    final ChannelPipeline p = ch.pipeline();
    p.addLast(new PortUnificationServerHandler(this.ctx));
    p.addLast(this.eventLogger);
  }

  /**
   * logging of user events.
   */

  @Sharable
  private static class UserEventLogger extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
      log.info("user event: " + evt);
      ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      if (cause instanceof IOException) {
        if (cause.getMessage().equals("Connection reset by peer")) {
          log.trace("client reset connection");
          return;
        }
        // TODO: log better
        log.debug("IOException: {}", cause.getMessage());
        return;
      }
      log.error("pipeline exception: {}", cause.getMessage(), cause);
      ctx.close();
    }

  }

}
