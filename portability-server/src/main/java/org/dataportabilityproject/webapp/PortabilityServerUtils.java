package org.dataportabilityproject.webapp;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

/**
 * Contains utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityServerUtils {

  /**
   * Returns whether or not the exchange is a HTTP GET request.
   * If not, it will close the client connection.
   */
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

  /**
   * Returns whether or not the exchange is a HTTP POST request.
   * If not, it will close the client connection.
   */
  public static boolean ValidatePostRequest(HttpExchange exchange) throws IOException{
    if(!exchange.getRequestMethod().equalsIgnoreCase("POST")){
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Not supported\n".getBytes());
      writer.close();
      return false;
    }
    return true;
  }

  /**
   * Returns map of request parameters from the provided HttpExchange.
   */
  public static Map<String, String> GetRequestParams(HttpExchange exchange) {
    URIBuilder builder = new URIBuilder(exchange.getRequestURI());
    List<NameValuePair> queryParamPairs= builder.getQueryParams();
    Map<String, String> params = new HashMap<String, String>();
    for(NameValuePair pair : queryParamPairs){
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }
}
