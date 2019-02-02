package zrz.webports.grpc;

import java.util.HashMap;
import java.util.Map;

import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingH2Stream;

public class GrpcRegistry {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrpcRegistry.class);

  private Map<String, ServiceInstance> methods = new HashMap<>();

  private class ServiceInstance {

    private GrpcInvoker<?, ?> invoker;

    public ServiceInstance(MethodDescriptor<?, ?> m, GrpcInvoker<?, ?> invoker) {
      this.descriptor = m;
      this.invoker = invoker;
    }

    MethodDescriptor<?, ?> descriptor;

    /**
     * dispatch the request.
     */

    public Flowable<Http2StreamFrame> handle(IncomingH2Stream in) {
      // return invoker.invoke(in.);
      
      //      in.incoming().
      
      //      in.incoming().redu
      
      return null;
    }

  }

  public GrpcRegistry() {
  }

  public <T> void register(ServiceDescriptor descriptor, T handler) {

    String name = descriptor.getName();

    log.info("registering gRPC service descriptor {}", name);

    GrpcReflectionInvoker invoker = new GrpcReflectionInvoker(handler);

    for (MethodDescriptor<?, ?> m : descriptor.getMethods()) {
      String path = "/" + m.getFullMethodName();
      methods.put(path, new ServiceInstance(m, invoker));
      log.info("registered gRPC service at {}", path, m.getType());
    }

  }

  public Flowable<Http2StreamFrame> handle(IncomingH2Stream in) {

    ServiceInstance method = this.methods.get(in.path().toString());

    if (method == null) {
      // no such method is registered.
      return null;
    }

    return method.handle(in);

  }

  public static GrpcRegistry create() {
    GrpcRegistry reg = new GrpcRegistry();
    return reg;
  }

}
