package zrz.webports.eventstream;

import io.reactivex.Flowable;

/**
 * provides a source of events.
 * 
 * @author theo
 *
 */

public interface EventSourceProvider {

  Flowable<EventSource> create(String path, String lastSeenEvent);

}
