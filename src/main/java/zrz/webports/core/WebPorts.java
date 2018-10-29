package zrz.webports.core;

import java.util.Optional;

import io.reactivex.Flowable;

public class WebPorts {

  /**
   * create a new builder.
   */

  public static SimpleWebPortBuilder simpleBuilder() {
    return new SimpleWebPortBuilder();
  }

  /**
   * supplier which returns the value of the environment setting, if available.
   *
   * @param envName
   *          The environment key
   * @return
   */

  public static Flowable<Optional<String>> fromEnv(final String key) {
    return Flowable.just(Optional.ofNullable(System.getenv(key)));
  }

  /**
   * supplier which returns a system property, if set.
   *
   * @param key
   * @return
   */

  public static Flowable<Optional<String>> fromProperty(final String key) {
    return Flowable.just(Optional.ofNullable(System.getenv(key)));
  }

}
