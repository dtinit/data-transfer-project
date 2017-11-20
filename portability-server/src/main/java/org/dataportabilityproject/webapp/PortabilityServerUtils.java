package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_COOKIE;

import com.google.common.base.Preconditions;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.LogUtils;

/**
 * Contains utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityServerUtils {

  /**
   * Returns the cookie specified in headers for the provided {@code} key.
   */
  public static String getCookie(Headers headers, String key) {
    Map<String, HttpCookie> cookieMap = getCookies(headers);
    HttpCookie httpCookie = cookieMap.get(key);
    String cookie = "";
    if (httpCookie != null) {
      cookie = httpCookie.getValue();
    }
    return cookie;
  }

  /**
   * Returns a Map of HttpCookies provided from the headers.
   */
  public static Map<String, HttpCookie> getCookies(Headers headers) {
    List<String> cookies = headers.get(HEADER_COOKIE);
    Map<String, HttpCookie> cookieMap = new HashMap<>();

    for (String cookieStr : cookies) {
      LogUtils.log("Cookie string: %s", cookieStr);
      for (HttpCookie httpCookie : HttpCookie.parse(cookieStr)) {
        cookieMap.put(httpCookie.getName(), httpCookie);
        LogUtils
            .log("parsed cookie, name: %s, value: %s", httpCookie.getName(), httpCookie.getValue());
      }
    }

    return cookieMap;
  }

  public static JsonObject createImportAuthJobResponse(String dataType, String exportService,
      String importService,
      String importAuthURL) {
    return Json.createObjectBuilder().add(JsonKeys.DATA_TYPE, dataType)
        .add(JsonKeys.EXPORT_SERVICE, exportService)
        .add(JsonKeys.IMPORT_SERVICE, importService)
        .add(JsonKeys.IMPORT_AUTH_URL, importAuthURL).build();
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

  /**
   * Returns map of request parameters from the provided HttpExchange.
   */
  public static Map<String, String> getRequestParams(HttpExchange exchange) {
    URIBuilder builder = new URIBuilder(exchange.getRequestURI());
    List<NameValuePair> queryParamPairs = builder.getQueryParams();
    Map<String, String> params = new HashMap<String, String>();
    for (NameValuePair pair : queryParamPairs) {
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }

  /**
   * Looks up job and checks that it exists in the provided jobManager.
   */
  public static PortabilityJob lookupJob(String id, JobManager jobManager) {
    PortabilityJob job = jobManager.findExistingJob(id);
    Preconditions.checkState(null != job, "existingJob not found for id: %s", id);
    return job;
  }

  /**
   * Returns whether or not the exchange is a valid request for the provided http method and
   * resource. If not, it will close the client connection.
   */
  public static boolean validateRequest(HttpExchange exchange, HttpMethods supportedMethod,
      String resourceRegex) throws IOException {
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
