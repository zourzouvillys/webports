package zrz.webports.eventstream;

import org.eclipse.jdt.annotation.Nullable;

public interface EventSource {

  @Nullable
  String id();

  @Nullable
  String event();

  @Nullable
  String data();

}
