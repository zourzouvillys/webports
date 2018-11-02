package zrz.webports.api;

/**
 * common HTTP request interface for HTTP/1.1 and H2 protocol requests.
 *
 * @author theo
 *
 */

public interface WebPortHttpRequest {

  /**
   *
   */

  CharSequence method();

  /**
   *
   */

  CharSequence path();

  /**
   * note that for HTTP/1.1, this will be emulated.
   */

  CharSequence scheme();

  /**
   * transport info.
   */

  WebPortTransportInfo transport();

}
