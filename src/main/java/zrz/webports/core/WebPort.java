package zrz.webports.core;

public interface WebPort {

  WebPort shutdown();

  void awaitTerminated();

}
