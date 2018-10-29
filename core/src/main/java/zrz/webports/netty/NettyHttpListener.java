package zrz.webports.netty;

import java.net.InetSocketAddress;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import zrz.webports.WebPortContext;

public class NettyHttpListener extends AbstractService {

  private final EventLoopGroup masterGroup = new NioEventLoopGroup(4);
  private final EventLoopGroup slaveGroup = new NioEventLoopGroup(4);

  private final List<ChannelFuture> channels = Lists.newArrayList();

  private final HttpServerConnector connector;
  private final int port;

  public NettyHttpListener(final int port, final WebPortContext ctx) {
    this.port = port;
    this.connector = new HttpServerConnector(ctx);
  }

  @Override
  protected void doStart() {

    final ServerBootstrap bootstrap = new ServerBootstrap()
        .group(this.masterGroup, this.slaveGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(this.connector.channelInitializer())
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

    try {
      this.channels.add(bootstrap.bind(this.port).sync());
      super.notifyStarted();
    }
    catch (final InterruptedException e) {
      super.notifyFailed(e);
    }

  }

  @Override
  protected void doStop() {

    this.slaveGroup.shutdownGracefully();
    this.masterGroup.shutdownGracefully();

    for (final ChannelFuture channel : this.channels) {
      try {
        channel.channel().closeFuture().sync();
      }
      catch (final InterruptedException e) {
      }
    }

    this.notifyStopped();

  }

  public HostAndPort listenHostAndPort() {
    return HostAndPort.fromParts(
        ((InetSocketAddress) this.channels.get(0).channel().localAddress()).getHostString(),
        this.listenPort());
  }

  public int listenPort() {
    return ((InetSocketAddress) this.channels.get(0).channel().localAddress()).getPort();
  }

}
