package zrz.webports.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class RxContextPropagator {

  private static AtomicBoolean INSTALLED = new AtomicBoolean();

  public static void ensureInstalled() {
    if (INSTALLED.compareAndSet(false, true)) {
      // RxJavaPlugins.setScheduleHandler(original -> Context.current().wrap(original));
    }
  }

}
