package zrz.webports.netty.sni;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsyncMapping;
import io.netty.util.concurrent.Future;

public class LocalSniHandler extends SniHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalSniHandler.class);

  public LocalSniHandler(AsyncMapping<? super String, ? extends SslContext> mapping) {
    super(mapping);
  }

  @Override
  protected Future<SslContext> lookup(ChannelHandlerContext ctx, String hostname) throws Exception {
    return mapping.map(hostname, ctx.executor().<SslContext>newPromise());
  }

}
