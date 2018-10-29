package zrz.webports.core.netty.inbound;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * limits the number of incoming connections allowed in total.
 * 
 * once we reach the mid watermark, we trigger an event to ask whatever is routing to us to (a) stop sending new
 * connections and (b) start new instances.
 * 
 * only once we hit the low wartermark so we again re-enable.
 * 
 * @author theo
 *
 */

@ChannelHandler.Sharable
public class MaxConnections extends ChannelInboundHandlerAdapter {

  private final int max;
  private final static AtomicInteger active = new AtomicInteger(0);

  public MaxConnections(int maxConnections) {
    this.max = maxConnections;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    int current = active.incrementAndGet();

    if (max > current) {
      Channel channel = ctx.channel();
      channel.close();
      ctx.pipeline().fireUserEventTriggered("connlimit");
    }

    super.channelActive(ctx);

  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    active.decrementAndGet();
    super.channelInactive(ctx);
  }

}
