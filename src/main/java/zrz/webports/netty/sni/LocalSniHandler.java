package zrz.webports.netty.sni;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsyncMapping;
import io.netty.util.concurrent.Future;
import zrz.webports.spi.SniProvider;

public class LocalSniHandler extends SniHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalSniHandler.class);

  public LocalSniHandler(final SniProvider mapping) {
    super((AsyncMapping<String, SslContext>) (input, promise) -> {

      mapping.map(input)
          .subscribe(promise::setSuccess, promise::setFailure);

      return promise;

    });
  }

  @Override
  protected Future<SslContext> lookup(final ChannelHandlerContext ctx, final String hostname) throws Exception {
    return this.mapping.map(hostname, ctx.executor().<SslContext>newPromise());
  }

}
