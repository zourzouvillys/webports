package zrz.webports.plugins.jersey2;

import java.io.IOException;
import java.io.InputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;

/**
 * exposes an input stream from a flowable bytebuf. kind of clunky and blocks :/
 */

class FlowableByteBufInputStream extends InputStream {

  private final Flowable<ByteBuf> in;
  private ByteBufInputStream buffer;

  public FlowableByteBufInputStream(final Flowable<ByteBuf> incoming) {
    this.in = incoming;
  }

  @Override
  public int read() throws IOException {
    this.slurp();
    return this.buffer.read();
  }

  @Override
  public void close() throws IOException {
    if (this.buffer != null) {
      this.buffer.close();
    }
  }

  private void slurp() {

    if (this.buffer != null) {
      return;
    }

    final ByteBuf buf =
      this.in
        .reduce(
          Unpooled.compositeBuffer(),
          (buffer, frame) -> {
            buffer.addComponent(true, frame);
            return buffer;
          })
        .blockingGet();

    this.buffer = new ByteBufInputStream(buf);

  }
}
