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

import static org.apache.axis.transport.http.HTTPConstants.HEADER_COOKIE;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.Config.Environment;
import org.dataportabilityproject.shared.LogUtils;

/**
 * Contains utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityServerUtils {

  /* Populates a JsonObject with the provided authorization data. */
  public static JsonObject createImportAuthJobResponse(String dataType, String exportService,
      String importService,
      String importAuthURL) {
    return Json.createObjectBuilder().add(JsonKeys.DATA_TYPE, dataType)
        .add(JsonKeys.EXPORT_SERVICE, exportService)
        .add(JsonKeys.IMPORT_SERVICE, importService)
        .add(JsonKeys.IMPORT_AUTH_URL, importAuthURL).build();
  }

  /* Returns a URL representing the resource provided.
   * TODO: remove hardcoded protocol - find a better way to do this from the HttpExchange.
   */
  public static String createURL(String protocol, String host, String URI) {
    String url = "";

    if (protocol.contains("HTTP/") && PortabilityFlags.environment() == Environment.LOCAL) {
      url = "http://" + host + URI;
    } else if (protocol.contains("HTTPS/")) {
      url = "https://" + host + URI;
    }

    Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Unsupported protocol");
    return url;
  }

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
      LogUtils.log("queryparam, name: %s, value: %s", pair.getName(), pair.getValue());
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }

  /**
   * Looks up job and checks that it exists in the provided jobDao.
   */
  public static PortabilityJob lookupJob(String id, JobDao jobDao) {
    PortabilityJob job = jobDao.findExistingJob(id);
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
