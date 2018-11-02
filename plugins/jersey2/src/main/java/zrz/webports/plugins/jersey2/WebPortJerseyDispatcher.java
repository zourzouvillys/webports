package zrz.webports.plugins.jersey2;

import java.util.concurrent.ExecutorService;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.spi.ExecutorServiceProvider;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.reactivex.Flowable;
import zrz.webports.api.IncomingH2Stream;
import zrz.webports.api.IncomingHttpRequest;

public class WebPortJerseyDispatcher implements Container {

  public static WebPortJerseyDispatcher fromConfig(final ResourceConfig config) {
    return new WebPortJerseyDispatcher(config);
  }

  private ApplicationHandler appHandler;

  protected WebPortJerseyDispatcher(final ResourceConfig resourceConfig) {
    this.appHandler = new ApplicationHandler(resourceConfig);
  }

  public InjectionManager injectionManager() {
    return this.appHandler.getInjectionManager();
  }

  /**
   * process an incoming H2 request.
   *
   * @param req
   * @param base
   * @param appHandler
   * @return
   */

  public Flowable<Http2StreamFrame> start(final IncomingH2Stream req, final String base) {
    return new Jersey2Http2Request(req, this, base).start();
  }

  public Flowable<HttpObject> start(final IncomingHttpRequest req, final String base) {
    return new Jersey2HttpRequest(req, this, base).start();
  }

  @Override
  public ResourceConfig getConfiguration() {
    return this.appHandler.getConfiguration();
  }

  @Override
  public ApplicationHandler getApplicationHandler() {
    return this.appHandler;
  }

  @Override
  public void reload() {
    this.reload(this.appHandler.getConfiguration());
  }

  @Override
  public void reload(final ResourceConfig configuration) {
    this.appHandler.onShutdown(this);
    this.appHandler = new ApplicationHandler(configuration);
    this.appHandler.onReload(this);
    this.appHandler.onStartup(this);
  }

  /**
   * this is called to dispatch a request in jersey. by default it executes directly. override to
   * dispatch using the ExecutorServiceProvider or some other logic.
   */

  protected void dispatch(final ContainerRequest requestContext) {
    this.appHandler.handle(requestContext);
  }

  protected ExecutorService executor() {
    return this.appHandler
      .getInjectionManager()
      .getInstance(ExecutorServiceProvider.class)
      .getExecutorService();
  }

}
