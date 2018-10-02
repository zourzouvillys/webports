package zrz.webports;

import org.junit.Test;

import zrz.webports.http.WebPortHttp;
import zrz.webports.http.WebPortWebSocket;

public class WebPortBuilderTest {

  @Test
  public void test() throws Exception {

    WebPorts.simpleBuilder()
        .listen(0)
        .h2(() -> req -> WebPortHttp.staticResponseH2(403, "Errr? (H2)"))
        .websocket(() -> req -> WebPortWebSocket.just("go away"))
        .http(() -> req -> WebPortHttp.staticResponse(404, "Errr?"))
        .build();

    Thread.sleep(10000);

  }

}
