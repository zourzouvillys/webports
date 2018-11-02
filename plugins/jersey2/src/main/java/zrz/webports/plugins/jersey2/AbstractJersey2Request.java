package zrz.webports.plugins.jersey2;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.PropertiesDelegate;

import zrz.webports.api.WebPortTransportInfo;

/**
 * common implementation of properties required for dispatching a jersey2 request.
 *
 * @author theo
 *
 */

public class AbstractJersey2Request implements PropertiesDelegate, SecurityContext {

  protected final WebPortJerseyDispatcher appHandler;
  protected final String base;

  private final WebPortTransportInfo transport;

  AbstractJersey2Request(
      final WebPortTransportInfo transport,
      final WebPortJerseyDispatcher appHandler,
      final String base) {

    this.appHandler = appHandler;
    this.base = base;
    this.transport = transport;

  }

  protected SecurityContext securityContext() {
    return this;
  }

  private final Map<String, Object> properties = new HashMap<>();

  @Override
  public Object getProperty(final String name) {
    return this.properties.get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return this.properties.keySet();
  }

  @Override
  public void setProperty(final String name, final Object object) {
    this.properties.put(name, object);
  }

  @Override
  public void removeProperty(final String name) {
    this.properties.remove(name);
  }

  @Override
  public boolean isUserInRole(final String role) {
    return false;
  }

  @Override
  public boolean isSecure() {
    return this.transport.isSecure();
  }

  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationScheme() {
    return null;
  }

}
