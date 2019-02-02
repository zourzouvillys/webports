package zrz.webports.core.netty.sni;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsyncMapping;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import zrz.webports.core.WebPortContext;
import zrz.webports.core.netty.h2.Http2OrHttpHandler;

public class LocalSniHandler extends SniHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalSniHandler.class);
  private WebPortContext ctx;

  public LocalSniHandler(final WebPortContext ctx) {
    super((AsyncMapping<String, SslContext>) (input, promise) -> {

      ctx.sni()
        .map(input)
        .subscribe(promise::setSuccess, promise::setFailure);

      return promise;

    });
    this.ctx = ctx;
  }

  @Override
  protected void replaceHandler(ChannelHandlerContext ctx, String hostname, SslContext sslContext) throws Exception {

    SslHandler sslHandler = null;

    try {
      sslHandler = sslContext.newHandler(ctx.alloc());
      
      System.err.println(sslHandler.engine());
      
      ctx.pipeline().replace(this, SslHandler.class.getName(), sslHandler);
      sslHandler = null;
      ctx.pipeline().addAfter(SslHandler.class.getName(), "Http2OrHttpHandler#0", new Http2OrHttpHandler(this.ctx));
    }
    finally {
      // Since the SslHandler was not inserted into the pipeline the ownership of the SSLEngine was
      // not
      // transferred to the SslHandler.
      // See https://github.com/netty/netty/issues/5678
      if (sslHandler != null) {
        ReferenceCountUtil.safeRelease(sslHandler.engine());
      }
    }

  }

  @Override
  protected Future<SslContext> lookup(final ChannelHandlerContext ctx, final String hostname) throws Exception {
    log.trace("looking up host for SNI {}", hostname);
    return this.mapping.map(hostname, ctx.executor().<SslContext>newPromise());
  }

}
