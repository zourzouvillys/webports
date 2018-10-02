package zrz.webports.spi;

import io.netty.handler.ssl.SslContext;
import io.reactivex.Flowable;

/**
 * When an incoming TLS ClientHello is received, it must be mapped to a local TLS context. This SPI interface is used to
 * perform the mapping.
 *
 * @author theo
 */

public interface SniProvider {

  Flowable<SslContext> map(String fqdn);

}
