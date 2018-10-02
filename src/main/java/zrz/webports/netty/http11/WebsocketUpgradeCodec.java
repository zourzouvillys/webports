package zrz.webports.netty.http11;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import zrz.webports.WebPortContext;
import zrz.webports.netty.wss.WebSocketFrameHandler;

/**
 * UpgradeCodec implementation for Websocket. only supports v13, and only over http/1.1 - websockets not supported over
 * h2 currently.
 *
 * @author theo
 *
 */

public class WebsocketUpgradeCodec implements UpgradeCodec {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebsocketUpgradeCodec.class);

  public static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
  private static final String SUB_PROTOCOL_WILDCARD = "*";

  private static String[] subprotocols = new String[] { "*" };

  private final WebPortContext ctx;

  private String selectedSubprotocol;

  public WebsocketUpgradeCodec(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public Collection<CharSequence> requiredUpgradeHeaders() {
    return ImmutableList.of();
  }

  @Override
  public boolean prepareUpgradeResponse(final ChannelHandlerContext ctx, final FullHttpRequest req, final HttpHeaders upgradeHeaders) {

    final CharSequence key = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_KEY);

    if (key == null) {
      throw new WebSocketHandshakeException(HttpHeaderNames.SEC_WEBSOCKET_KEY + " header is required");
    }

    final String subprotocols = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);

    if (subprotocols != null) {

      this.selectedSubprotocol = this.selectSubprotocol(subprotocols);

      if (this.selectedSubprotocol == null) {

        if (log.isDebugEnabled()) {
          log.debug("Requested subprotocol(s) not supported: {}", subprotocols);
        }

        // no supported protocol. we should reject it.
        return false;

      }

    }

    final String acceptSeed = key + WEBSOCKET_13_ACCEPT_GUID;
    final byte[] sha1 = sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    final String accept = base64(sha1);

    if (log.isDebugEnabled()) {
      log.debug("WebSocket version 13 server handshake key: {}, response: {}", key, accept);
    }

    if (this.selectedSubprotocol != null) {
      upgradeHeaders.add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, this.selectedSubprotocol);
    }

    upgradeHeaders.add(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, accept);

    return true;

  }

  private String selectSubprotocol(final String requestedSubprotocols) {

    if ((requestedSubprotocols == null) || (subprotocols.length == 0)) {
      return null;
    }

    final String[] requestedSubprotocolArray = requestedSubprotocols.split(",");

    for (final String p : requestedSubprotocolArray) {

      final String requestedSubprotocol = p.trim();

      for (final String supportedSubprotocol : subprotocols) {
        if (SUB_PROTOCOL_WILDCARD.equals(supportedSubprotocol) || requestedSubprotocol.equals(supportedSubprotocol)) {
          return requestedSubprotocol;
        }
      }
    }

    // No match found
    return null;
  }

  static String base64(final byte[] data) {
    return BaseEncoding.base64().encode(data);
  }

  @SuppressWarnings("deprecation")
  static byte[] md5(final byte[] data) {
    return Hashing.md5().hashBytes(data).asBytes();
  }

  @SuppressWarnings("deprecation")
  static byte[] sha1(final byte[] data) {
    return Hashing.sha1().hashBytes(data).asBytes();
  }

  @Override
  public void upgradeTo(final ChannelHandlerContext ctx, final FullHttpRequest req) {

    final Channel channel = ctx.channel();

    if (log.isDebugEnabled()) {
      log.debug("{} WebSocket version 13 server handshake", channel);
    }

    final ChannelPipeline p = channel.pipeline();

    if (p.get(Http1Initializer.class) != null) {
      p.remove(Http1Initializer.class);
    }

    final UnicastProcessor<WebSocketFrame> rxqueue = UnicastProcessor.create();

    final Flowable<WebSocketFrame> handler = this.ctx.websocket(() -> rxqueue);

    p.addAfter(ctx.name(), "wshandler", new WebSocketFrameHandler(this.ctx, this.selectedSubprotocol, handler, rxqueue));
    p.addAfter(ctx.name(), "wsdecoder", this.newWebsocketDecoder());
    p.addAfter(ctx.name(), "wsencoder", this.newWebSocketEncoder());

    handler.subscribe(
        frame -> channel.writeAndFlush(frame),
        err -> {
          log.warn("flow error: {}", err.getMessage(), err);
          channel.close();
        },
        () -> {
          channel.close();
        });

  }

  private WebSocketFrameDecoder newWebsocketDecoder() {
    return new WebSocket13FrameDecoder(true, true, this.maxFramePayloadLength(), this.allowMaskMismatch());
  }

  private boolean allowMaskMismatch() {
    return false;
  }

  private int maxFramePayloadLength() {
    return 65535;
  }

  private WebSocketFrameEncoder newWebSocketEncoder() {
    return new WebSocket13FrameEncoder(false);
  }

}
