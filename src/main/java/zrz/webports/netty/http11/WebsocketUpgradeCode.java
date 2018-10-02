package zrz.webports.netty.http11;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.reactivex.Flowable;
import zrz.webports.WebPortContext;
import zrz.webports.spi.IncomingWebSocket;

/**
 * UpgradeCodec implementation for Websocket. only supports v13.
 *
 * @author theo
 *
 */

public class WebsocketUpgradeCode implements UpgradeCodec, IncomingWebSocket {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebsocketUpgradeCode.class);

  private final WebPortContext ctx;

  private String selectedSubprotocol;

  public WebsocketUpgradeCode(final WebPortContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public Collection<CharSequence> requiredUpgradeHeaders() {
    return ImmutableList.of();
  }

  public static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private static final String SUB_PROTOCOL_WILDCARD = "*";

  @Override
  public boolean prepareUpgradeResponse(final ChannelHandlerContext ctx, final FullHttpRequest req, final HttpHeaders upgradeHeaders) {

    final CharSequence key = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_KEY);

    if (key == null) {
      throw new WebSocketHandshakeException("not a WebSocket request: missing key");
    }

    final String acceptSeed = key + WEBSOCKET_13_ACCEPT_GUID;
    final byte[] sha1 = sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    final String accept = base64(sha1);

    if (log.isDebugEnabled()) {
      log.debug("WebSocket version 13 server handshake key: {}, response: {}", key, accept);
    }

    upgradeHeaders.add(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET);
    upgradeHeaders.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
    upgradeHeaders.add(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, accept);

    final String subprotocols = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);

    if (subprotocols != null) {

      final String selectedSubprotocol = this.selectSubprotocol(subprotocols);

      if (selectedSubprotocol == null) {
        if (log.isDebugEnabled()) {
          log.debug("Requested subprotocol(s) not supported: {}", subprotocols);
        }
      }
      else {
        upgradeHeaders.add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, selectedSubprotocol);
      }

    }

    return true;

  }

  private static String[] subprotocols = new String[] {};

  private String selectSubprotocol(final String requestedSubprotocols) {

    if ((requestedSubprotocols == null) || (subprotocols.length == 0)) {
      return null;
    }

    final String[] requestedSubprotocolArray = requestedSubprotocols.split(",");
    for (final String p : requestedSubprotocolArray) {
      final String requestedSubprotocol = p.trim();

      for (final String supportedSubprotocol : subprotocols) {
        if (SUB_PROTOCOL_WILDCARD.equals(supportedSubprotocol)
            || requestedSubprotocol.equals(supportedSubprotocol)) {
          this.selectedSubprotocol = requestedSubprotocol;
          return requestedSubprotocol;
        }
      }
    }

    // No match found
    return null;
  }

  /**
   * Performs base64 encoding on the specified data
   *
   * @param data
   *          The data to encode
   * @return An encoded string containing the data
   */
  static String base64(final byte[] data) {
    final ByteBuf encodedData = Unpooled.wrappedBuffer(data);
    final ByteBuf encoded = Base64.encode(encodedData);
    final String encodedString = encoded.toString(CharsetUtil.UTF_8);
    encoded.release();
    return encodedString;
  }

  private static final FastThreadLocal<MessageDigest> MD5 = new FastThreadLocal<>() {
    @Override
    protected MessageDigest initialValue() throws Exception {
      try {
        // Try to get a MessageDigest that uses MD5
        return MessageDigest.getInstance("MD5");
      }
      catch (final NoSuchAlgorithmException e) {
        // This shouldn't happen! How old is the computer?
        throw new InternalError("MD5 not supported on this platform - Outdated?");
      }
    }
  };

  private static final FastThreadLocal<MessageDigest> SHA1 = new FastThreadLocal<>() {
    @Override
    protected MessageDigest initialValue() throws Exception {
      try {
        // Try to get a MessageDigest that uses SHA1
        return MessageDigest.getInstance("SHA1");
      }
      catch (final NoSuchAlgorithmException e) {
        // This shouldn't happen! How old is the computer?
        throw new InternalError("SHA-1 not supported on this platform - Outdated?");
      }
    }
  };

  /**
   * Performs a MD5 hash on the specified data
   *
   * @param data
   *          The data to hash
   * @return The hashed data
   */
  static byte[] md5(final byte[] data) {
    // TODO(normanmaurer): Create md5 method that not need MessageDigest.
    return digest(MD5, data);
  }

  /**
   * Performs a SHA-1 hash on the specified data
   *
   * @param data
   *          The data to hash
   * @return The hashed data
   */
  static byte[] sha1(final byte[] data) {
    // TODO(normanmaurer): Create sha1 method that not need MessageDigest.
    return digest(SHA1, data);
  }

  private static byte[] digest(final FastThreadLocal<MessageDigest> digestFastThreadLocal, final byte[] data) {
    final MessageDigest digest = digestFastThreadLocal.get();
    digest.reset();
    return digest.digest(data);
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

    p.addAfter(ctx.name(), "wsdecoder", this.newWebsocketDecoder());
    p.addAfter(ctx.name(), "wsencoder", this.newWebSocketEncoder());

    final Flowable<WebSocketFrame> handler = this.ctx.websocket(this);

    handler.subscribe(
        frame -> {
          ctx.channel().writeAndFlush(frame);
        },
        err -> {
          // TODO: handle in annother way?
          log.warn("got error on txmit flowable: {}", err.getMessage(), err);
          ctx.channel().close();
        },
        () -> {
          log.debug("H2 txstream completed");
          ctx.channel().close();
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
