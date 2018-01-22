/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject.webapp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server that handles requests to API/web server. */
final class ApiServer {
  private final Logger logger = LoggerFactory.getLogger(ApiServer.class);

  private static final int PORT = 8080;

  private final HttpServer server;

  @Inject
  ApiServer(Map<String, HttpHandler> handlers) throws IOException {
    // TODO: backlog and port should be command line args
    server = HttpServer.create(new InetSocketAddress(PORT), 0);
    for (Entry<String, HttpHandler> entry : handlers.entrySet()) {
      server.createContext(entry.getKey(), entry.getValue());
    }

    // Redirect anything that doesn't match to the ViewHandler. The view handler serves index.html
    // which should reference static content served by our bucket. The angular app then routes requests
    // client side via angular.
    server.createContext("/", new ViewHandler());

    ThreadFactory threadPoolFactory = new ThreadFactoryBuilder()
        .setNameFormat("http-server-%d")
        .setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit())
        .build();
    server.setExecutor(Executors.newCachedThreadPool(threadPoolFactory));
  }

  public void start() throws IOException {
    server.start();

    logger.info("Server is listening on port {}", PORT);
  }
}
