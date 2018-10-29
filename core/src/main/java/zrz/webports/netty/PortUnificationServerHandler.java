package zrz.webports.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslHandler;
import zrz.webports.WebPortContext;
import zrz.webports.netty.h2.Http2OrHttpHandler;
import zrz.webports.netty.http11.Http1Initializer;
import zrz.webports.netty.sni.LocalSniHandler;

/**
 * handles protocol detection to enable TLS and HTTP on the same port.
 *
 * @author theo
 *
 */

public class PortUnificationServerHandler extends ByteToMessageDecoder {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PortUnificationServerHandler.class);
  private final WebPortContext ctx;

  public PortUnificationServerHandler(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {

    if (in.readableBytes() < 5) {
      // need at least 5 bytes to sniff.
      return;
    }

    if (this.isSsl(in)) {
      this.enableSsl(ctx);

    }
    else {

      final int magic1 = in.getUnsignedByte(in.readerIndex());
      final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);

      if (isHttp(magic1, magic2)) {

        this.switchToHttp(ctx);

      }
      else {
        // Unknown protocol; discard everything and close the connection.
        in.clear();
        ctx.close();
      }

    }

  }

  private boolean isSsl(final ByteBuf buf) {
    return SslHandler.isEncrypted(buf);
  }

  private static boolean isHttp(final int magic1, final int magic2) {
    return ((magic1 == 'G') && (magic2 == 'E')) || // GET
        ((magic1 == 'P') && (magic2 == 'O')) || // POST
        ((magic1 == 'P') && (magic2 == 'U')) || // PUT
        ((magic1 == 'H') && (magic2 == 'E')) || // HEAD
        ((magic1 == 'O') && (magic2 == 'P')) || // OPTIONS
        ((magic1 == 'P') && (magic2 == 'A')) || // PATCH
        ((magic1 == 'D') && (magic2 == 'E')) || // DELETE
        ((magic1 == 'T') && (magic2 == 'R')) || // TRACE
        ((magic1 == 'C') && (magic2 == 'O')); // CONNECT
  }

  private void enableSsl(final ChannelHandlerContext ctx) {

    final ChannelPipeline p = ctx.pipeline();

    p.addAfter("PortUnificationServerHandler#0", "LocalSniHandler#0", new LocalSniHandler(this.ctx.sni()));
    // TLS can have both http/1.1 or h2.
    p.addLast(new Http2OrHttpHandler(this.ctx));

    p.remove(this);

    log.trace("enabled SSL, {}", p.names());

  }

  private void switchToHttp(final ChannelHandlerContext ctx) {
    final ChannelPipeline p = ctx.pipeline();
    p.addLast(new Http1Initializer(this.ctx));
    p.remove(this);
  }

}
