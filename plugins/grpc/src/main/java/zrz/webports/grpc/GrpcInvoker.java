package zrz.webports.grpc;

import io.reactivex.Flowable;

@FunctionalInterface
public interface GrpcInvoker<InT, OutT> {

  Flowable<OutT> invoke(Flowable<InT> in);

}
