package zrz.webports.core.utils;

import zrz.webports.api.IncomingHttpRequest;

/**
 * a simple path based router which dispatches to components based on the request path.
 *
 * @author theo
 *
 */

public class PathRouter {

  public void accept(final IncomingHttpRequest req) {
    this.process(req.path(), req);
  }

  /**
   *
   * @param path
   * @param req
   */

  public void process(final CharSequence path, final IncomingHttpRequest req) {
  }

}
