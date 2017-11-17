package org.dataportabilityproject.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

/**
 * Contains utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityServerUtils {

  /**
   * Returns whether or not the exchange is a valid request for the provided
   * http method and resource.
   * If not, it will close the client connection.
   */
  public static boolean validateRequest(HttpExchange exchange, HttpMethods supportedMethod,
      String resourceRegex) throws IOException{
    String path = exchange.getRequestURI().getPath();

    if (!exchange.getRequestMethod().equalsIgnoreCase(supportedMethod.name())) {
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Method not supported\n".getBytes());
      writer.close();
      return false;
    } else if (!Pattern.compile(resourceRegex).matcher(path).matches()) {
      LogUtils.log("Path: %s, pattern: %s", path, resourceRegex);
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Not found\n".getBytes());
      writer.close();
      return false;
    }
    return true;
  }

  /**
   * Returns map of request parameters from the provided HttpExchange.
   */
  public static Map<String, String> getRequestParams(HttpExchange exchange) {
    URIBuilder builder = new URIBuilder(exchange.getRequestURI());
    List<NameValuePair> queryParamPairs= builder.getQueryParams();
    Map<String, String> params = new HashMap<String, String>();
    for(NameValuePair pair : queryParamPairs){
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }

  /**
   * Returns map of request parameters from the RequestBody of HttpExchange.
   */
  public static Map<String, String> getPostParams(HttpExchange exchange) {
    Map<String, String> requestParameters = new HashMap<>();

    // postParams are provided in the form of:
    // dataType=1%3A+CALENDAR&exportService=1%3A+Google&importService=2%3A+Microsoft
    String postParams = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
        .lines().collect(Collectors.joining("\n"));
    LogUtils.log("post parameters: %s", postParams);

    parsePostParams(postParams, requestParameters);
    for (Entry<String, String> param : requestParameters.entrySet()) {
      LogUtils.log("param %s : %s", param.getKey(), param.getValue());
    }

    return requestParameters;
  }

  // TODO: figure out how to get the client to submit "clean" values
  // Hack to strip the angular indexing in option values.
  private static void parsePostParams(String postParams, Map<String, String> params) {
    String[] trimmed = postParams.split("&");
    for (String s : trimmed) {
      String[] keyval = s.split("=");
      String value = keyval[1];
      value = value.substring(value.indexOf("+") + 1).trim();
      params.put(keyval[0], value);
    }
  }

}
