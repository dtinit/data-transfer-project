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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

// TODO: Migrate to Launcher API
/** Server that handles requests to API/web server using the Sun HttpServer framework. */
@Singleton
public final class ReferenceApiServer {
  private final Logger logger = LoggerFactory.getLogger(ReferenceApiServer.class);
  private final int port;
  private final HttpServer server;

  @Inject
  ReferenceApiServer(
      Map<String, HttpHandler> handlers,
      @Named("httpPort") int port,
      @Named("defaultView") String defaultView,
      @Named("httpExecutor") Executor httpExecutor)
      throws IOException {
    // TODO: backlog and port should be command line args
    this.port = port;
    this.server = createServer(port);
    setHandlers(handlers);
    setDefaultView(defaultView);
    setExecutor(httpExecutor);
  }

  private static HttpServer createServer(int port) throws IOException {
    return HttpServer.create(new InetSocketAddress(port), 0);
  }

  /** Starts the Sun HttpServer based API. */
  public void start() throws IOException {
    server.start();
    logger.info("Server is listening on port {}", port);
  }

  public void stop() {
    server.stop(0);
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
