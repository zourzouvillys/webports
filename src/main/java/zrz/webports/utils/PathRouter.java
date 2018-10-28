package zrz.webports.utils;

import zrz.webports.api.IncomingHttpRequest;

/**
 * a simple path based router which dispatches to components based on the request path.
 * 
 * @author theo
 *
 */

public class PathRouter {

  public void accept(IncomingHttpRequest req) {
    process(req.path(), req);
  }

  /**
   * 
   * @param path
   * @param req
   */

  public void process(String path, IncomingHttpRequest req) {
  }

}
