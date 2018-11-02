package zrz.webports.api;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

/**
 * common API wrapper for HTTP/1.1 and H2 specific streams which are HTTP based.
 *
 * @author theo
 *
 */

public interface WebPortHttpHeaders extends Iterable<Map.Entry<CharSequence, CharSequence>> {

  CharSequence get(CharSequence headerName);

  Iterable<@NonNull ? extends CharSequence> getAll(CharSequence headerName);

}
