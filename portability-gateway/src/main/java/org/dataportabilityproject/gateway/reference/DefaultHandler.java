/*
* Copyright 2018 The Data-Portability Project Authors.
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

import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that serves the given {@code resource}.
 */
public class DefaultHandler implements HttpHandler {
  private final Logger logger = LoggerFactory.getLogger(DefaultHandler.class);

  /** The resource, e.g. html file, to serve from this handler. */
  private final String resource;

  public DefaultHandler(String resource) {
    this.resource = resource;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    logger.debug("Got request {}", exchange.getRequestURI().toString());
    InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resource);

    if (resourceStream == null) {
      // If resource doesn't exist: reject with 404 error.
      logger.error("Could not open resource: {}", resource);
      String response = "404 (Not Found)\n";
      exchange.sendResponseHeaders(404, response.length());
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else {
      logger.debug("Found resource: {}", resource);
      exchange.sendResponseHeaders(200, 0);
      OutputStream out = exchange.getResponseBody();
      ByteStreams.copy(resourceStream, out);
      resourceStream.close();
      out.close();
    }
  }
}
