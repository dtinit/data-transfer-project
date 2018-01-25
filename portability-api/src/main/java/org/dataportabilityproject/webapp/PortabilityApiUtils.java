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
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.ServiceMode;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.parse.CookieParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utility functions for use by the PortabilityServer HttpHandlers
 */
public class PortabilityApiUtils {

  /**
   * Attributes to attach to all cookies set by the API - Since HttpCookie doesnt support adding
   * arbitrary attributes, we need to do this manually by concatenating to the cookie string.
   *
   * SameSite=lax specification allows cookies to be sent by the browser on top level GET requests
   * and on requests from within the app.
   */
  public final static String COOKIE_ATTRIBUTES = "; Path=/; SameSite=lax";
  private static final Logger logger = LoggerFactory.getLogger(PortabilityApiUtils.class);

  /**
   * Returns a URL representing the resource provided. TODO: remove hardcoded scheme - find a better
   * way to do this from the HttpExchange.
   */
  public static String createURL(String host, String URI, boolean useHttps) {
    // http is only allowed if this is running a local instance, enforce https instead.
    String scheme = useHttps ? "https://" : "http://";

    logger.debug("createURL, scheme: {}, host: {}, URI: {}", scheme, host, URI);
    return scheme + host + URI;

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
      logger.debug("Cookie string: {}", cookieStr);
      CookieParser parser = new CookieParser(cookieStr);
      for (Cookie c : parser) {
        HttpCookie httpCookie = new HttpCookie(c.getName(), c.getValue());
        logger.debug("parsed cookie, name: {}, value: {}",
            httpCookie.getName(), httpCookie.getValue());
        cookieMap.put(httpCookie.getName(), httpCookie);
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
    logger.debug("post parameters: {}", postParams);

    parsePostParams(postParams, requestParameters);
    for (Entry<String, String> param : requestParameters.entrySet()) {
      logger.debug("param {} : {}", param.getKey(), param.getValue());
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
      logger.debug("queryparam, name: {}, value: {}", pair.getName(), pair.getValue());
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }

  /**
   * Looks up job in pending auth data state and verifies it exists.
   */
  public static PortabilityJob lookupJobPendingAuthData(String id, JobDao jobDao) {
    PortabilityJob job = jobDao.lookupJobPendingAuthData(id);
    Preconditions.checkNotNull(job, "existingJob not found for id: %s", id);
    return job;
  }

  /**
   * Looks up job and checks that it exists in the provided jobDao.
   */
  public static PortabilityJob lookupJob(String id, JobDao jobDao) {
    PortabilityJob job = jobDao.findExistingJob(id);
    Preconditions.checkNotNull(job, "existingJob not found for id: %s", id);
    return job;
  }

  /**
   * Hack! For now, if we don't have export auth data, assume it's for export.
   */
  public static ServiceMode getServiceMode(PortabilityJob job, Headers headers,
      boolean useEncryptedFlow) {
    if (useEncryptedFlow) {
      String exportAuthCookie = PortabilityApiUtils
          .getCookie(headers, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
      return (Strings.isNullOrEmpty(exportAuthCookie) ? ServiceMode.EXPORT : ServiceMode.IMPORT);
    } else {
      return (null == job.exportAuthData() ? ServiceMode.EXPORT : ServiceMode.IMPORT);
    }
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
      logger.debug("Path: {}, pattern: {}", path, resourceRegex);
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Not found\n".getBytes());
      writer.close();
      return false;
    }
    return true;
  }

  /**
   * Validates that the JobId in the request matches the jobId in the xsrf header and contains
   * Does not validate that the job id itself is valid. Returns JobID.
   */
  public static String validateJobId(Headers requestHeaders, TokenManager tokenManager) {
    String encodedIdCookie = PortabilityApiUtils
        .getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);

    // Validate XSRF token is present in request header and in the token.
    String tokenHeader = parseXsrfTokenHeader(requestHeaders);
    String tokenCookie = PortabilityApiUtils
        .getCookie(requestHeaders, JsonKeys.XSRF_TOKEN);

    // Both header and token should be present
    Preconditions.checkArgument(!Strings.isNullOrEmpty(tokenHeader), "xsrf token header must be present");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(tokenCookie), "xsrf token cookie must be present");

    // The token present in the header should be the same as the token present in the cookie.
    Preconditions.checkArgument(tokenCookie.equals(tokenHeader), "xsrf token header and cookie must match");

    // Verify that the token is actually valid in the tokenManager
    Preconditions.checkArgument(tokenManager.verifyToken(tokenHeader), "xsrf token provided is invalid");

    // finally make sure the jobId present in the token is also equal to the jobId present in the cookie
    String jobIdFromToken = tokenManager.getData(tokenHeader);
    Preconditions.checkArgument(jobId.equals(jobIdFromToken), "encoded job id and job id token must match");
    return jobId;
  }

  //The cookie value might be surrounded by double quotes which causes the angular cli to also
  // surround the header with double quotes. Since the value itself may not contain quotes or
  // whitespace, trim off the double quotes by converting them to whitespace.
  private static String parseXsrfTokenHeader(Headers requestHeaders){
    return requestHeaders.getFirst(JsonKeys.XSRF_HEADER)
        .replace("\"", " ")
        .trim();
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
