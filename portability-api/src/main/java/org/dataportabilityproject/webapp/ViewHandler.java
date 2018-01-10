/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to redirect all paths to the angular view.
 */
public class ViewHandler implements HttpHandler {
  private final Logger logger = LoggerFactory.getLogger(ViewHandler.class);

  private final String INDEX = "static/index.html";

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    logger.debug("Got request {}", exchange.getRequestURI().toString());
    InputStream index_stream = getClass().getClassLoader().getResourceAsStream(INDEX);

    if (index_stream == null) {
      // Index file doesn't exist: reject with 404 error.
      logger.error("Could not open file: {}", INDEX);
      String response = "404 (Not Found)\n";
      exchange.sendResponseHeaders(404, response.length());
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else {
      logger.debug("Found file: {}", INDEX);
      exchange.sendResponseHeaders(200, 0);
      OutputStream os = exchange.getResponseBody();
      final byte[] buffer = new byte[0x10000];
      int count = 0;
      while ((count = index_stream.read(buffer)) >= 0) {
        os.write(buffer, 0, count);
      }
      os.close();
      index_stream.close();
    }
  }
}
