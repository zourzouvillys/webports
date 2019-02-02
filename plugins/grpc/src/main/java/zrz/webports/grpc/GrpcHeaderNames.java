package zrz.webports.grpc;

import io.netty.util.AsciiString;

public final class GrpcHeaderNames {

  // @formatter:off
  public static final AsciiString GRPC_ACCEPT_ENCODING = AsciiString.cached("grpc-accept-encoding");
  public static final AsciiString GRPC_ENCODING        = AsciiString.cached("grpc-encoding");
  public static final AsciiString GRPC_MESSAGE         = AsciiString.cached("grpc-message");
  public static final AsciiString GRPC_STATUS          = AsciiString.cached("grpc-status");
  public static final AsciiString GRPC_TIMEOUT         = AsciiString.cached("grpc-timeout");
  // @formatter:on

  private GrpcHeaderNames() {
  }

}
