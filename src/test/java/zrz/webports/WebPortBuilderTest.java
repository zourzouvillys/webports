package zrz.webports;

import org.junit.Test;

import io.netty.handler.codec.http.HttpObject;
import io.reactivex.Flowable;
import zrz.webports.http.WebPortHttp;
import zrz.webports.http.WebPortWebSocket;
import zrz.webports.spi.IncomingH2Stream;

public class WebPortBuilderTest {

  @Test
  public void test() throws Exception {

    WebPorts.simpleBuilder()
        .listen(9991)
        .h2(() -> req -> handler(req).flatMap(WebPortHttp::toHttp2))
        .websocket(() -> req -> {

          req.incoming()
              .subscribe(msg -> System.err.println(msg), err -> System.err.println(err));

          return WebPortWebSocket.just("go away").concatWith(Flowable.never());

        })
        .http(() -> req -> WebPortHttp.staticResponse(404, "Errr?"))
        .build();

  }

  static Flowable<HttpObject> handler(final IncomingH2Stream req) {
    return WebPortHttp.staticResponse(401, "Errr?");
  }

}
