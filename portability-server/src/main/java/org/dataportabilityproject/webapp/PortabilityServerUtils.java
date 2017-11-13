package org.dataportabilityproject.webapp;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;

/**
 * Contains Utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityServerUtils {
  // Returns whether or not the exchange is a HTTP GET request.
  // If not, it will close the client connection
  public static boolean ValidateGetRequest(HttpExchange exchange) throws IOException{
    if(!exchange.getRequestMethod().equalsIgnoreCase("GET")){
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Not supported\n".getBytes());
      writer.close();
      return false;
    }
    return true;
  }
}
