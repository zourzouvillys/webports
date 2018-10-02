package zrz.webports;

import org.junit.Test;

import io.reactivex.Flowable;
import zrz.webports.http.WebPortHttp;
import zrz.webports.http.WebPortWebSocket;

public class WebPortBuilderTest {

  @Test
  public void test() throws Exception {

    WebPorts.simpleBuilder()
        .listen(0)
        .h2(() -> req -> WebPortHttp.staticResponseH2(403, "Errr? (H2)"))
        .websocket(() -> req -> {

          req.incoming()
              .subscribe(msg -> System.err.println(msg), err -> System.err.println(err));

          return WebPortWebSocket.just("go away").concatWith(Flowable.never());

        })
        .http(() -> req -> WebPortHttp.staticResponse(404, "Errr?"))
        .build();

  }

}
