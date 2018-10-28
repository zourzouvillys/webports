# [WIP] H2/TLS & WebSocket to RxJava2

[![](https://jitpack.io/v/zouxrzouvillys/webports.svg)](https://jitpack.io/#zourzouvillys/webports)

Provides a simple API for embedding http, h2 and websocket service in an application with SNI based TLS.

Using netty directly to set up a http/1.1, h2, tls, and websocket application isn't a one-liner.  The other option is to use
a higher level framework like JAX-RS or Servlets. WebPorts sits somewhere in the middle between the low level netty model
and a high level framework - it is a lightweight utility for wiring up netty with your own code.

It also provides a common API for HTTP/1.1 serving over HTTP and H2 transports.



```gradle
repositories {
  ...
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.zourzouvillys:webports-runtime:master-SNAPSHOT'
}
```

## Packaging

WebPorts is split up into a few packages so you don't have to pull in more dependencies than you need.

- runtime - main processing engine. only needed by code actually starting serving. automatically includes api/sni.
- api - only dependency needed for processing incoming requests. has no dependencies.  
- spi - for extending the server behavior
- plugins/* - for server enhancements

If your own application is split into modules/libs, then you only need to add the api as a dependency:

```
  implementation 'com.github.zourzouvillys:webports-api:master-SNAPSHOT'
```

