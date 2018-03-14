/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.gateway.reference;

import com.google.inject.Inject;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

// TODO: Migrate to Launcher API
/** Server that handles requests to API/web server using the Sun HttpServer framework. */
@Singleton
public final class ReferenceApiServer {
  private final Logger logger = LoggerFactory.getLogger(ReferenceApiServer.class);
  private final Map<String, HttpHandler> handlers;
  private final int port;
  private final String defaultView;
  private final Executor httpExecutor;
  private HttpServer server;

  @Inject(optional = true)
  public KeyManagerFactory keyManagerFactory;

  @Inject(optional = true)
  public TrustManagerFactory trustManagerFactory;

  @Inject
  ReferenceApiServer(
      Map<String, HttpHandler> handlers,
      @Named("httpPort") int port,
      @Named("defaultView") String defaultView,
      @Named("httpExecutor") Executor httpExecutor) {
    this.handlers = handlers;
    // TODO: backlog and port should be command line args
    this.port = port;
    this.defaultView = defaultView;
    this.httpExecutor = httpExecutor;
  }

  /** Starts the Sun HttpServer based API. */
  public void start() throws Exception {
    this.server = createServer(port);
    setHandlers(handlers);
    setDefaultView(defaultView);
    setExecutor(httpExecutor);

    server.start();
    logger.info("Server is listening on port {}", port);
  }

  public void stop() {
    server.stop(0);
  }

  private HttpServer createServer(int port) throws Exception {
      if (trustManagerFactory == null || keyManagerFactory == null) {
        return HttpServer.create(new InetSocketAddress(port), 0);
      }

      HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);
      SSLContext sslContext = SSLContext.getInstance("TLS");

      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

      server.setHttpsConfigurator(
              new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                  try {
                    SSLContext context = SSLContext.getDefault();
                    SSLEngine engine = context.createSSLEngine();
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    SSLParameters defaultSSLParameters = context.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);

                    params.setNeedClientAuth(false);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              });
      return server;
  }

  private void setHandlers(Map<String, HttpHandler> handlers) {
    for (Entry<String, HttpHandler> entry : handlers.entrySet()) {
      server.createContext(entry.getKey(), entry.getValue());
    }
  }

  // Redirect anything that doesn't match to the ViewHandler. The view handler serves index.html
  // which should reference static content served by our bucket. The angular app then routes
  // requests
  // client side via angular.
  private void setDefaultView(String resource) {
    server.createContext("/", new DefaultHandler(resource));
  }

  private void setExecutor(Executor executor) {
    server.setExecutor(executor);
  }
}
