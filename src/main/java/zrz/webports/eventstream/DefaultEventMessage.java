package zrz.webports.eventstream;

import org.eclipse.jdt.annotation.Nullable;

public class DefaultEventMessage implements EventSource {

  private final String id;
  private final String event;
  private final String data;

  public DefaultEventMessage(Object id, String event, String data) {
    if (id != null)
      this.id = id.toString();
    else
      this.id = null;
    this.event = event;
    this.data = data;
  }

  @Override
  public @Nullable String id() {
    return this.id;
  }

  @Override
  public @Nullable String event() {
    return this.event;
  }

  @Override
  public @Nullable String data() {
    return this.data;
  }

}
