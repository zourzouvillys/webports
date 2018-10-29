package zrz.webports;

public interface WebPort {

  WebPort shutdown();

  void awaitTerminated();

}
