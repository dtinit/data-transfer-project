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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.parse.CookieParser;

/** Contains utility functions for use by the ReferenceApiServer HttpHandlers */
public final class ReferenceApiUtils {

  /**
   * Attributes to attach to all cookies set by the API - Since HttpCookie doesnt support adding
   * arbitrary attributes, we need to do this manually by concatenating to the cookie string.
   *
   * <p>SameSite=lax specification allows cookies to be sent by the browser on top level GET
   * requests and on requests from within the app.
   */
  public static final String COOKIE_ATTRIBUTES = "; Path=/; SameSite=lax";

  /**
   * Returns a URL representing the resource provided. TODO: remove hardcoded scheme - find a better
   * way to do this from the HttpExchange.
   */
  public static String createURL(String host, String URI, boolean useHttps) {
    // http is only allowed if this is running a local instance, enforce https instead.
    String scheme = useHttps ? "https://" : "http://";
    return scheme + host + URI;
  }

  /** Returns the cookie specified in headers for the provided {@code} key. */
  public static String getCookie(Headers headers, String key) {
    Map<String, HttpCookie> cookieMap = getCookies(headers);
    HttpCookie httpCookie = cookieMap.get(key);
    String cookie = "";
    if (httpCookie != null) {
      cookie = httpCookie.getValue();
    }
    return cookie;
  }

  /** Returns a Map of HttpCookies provided from the headers. */
  public static Map<String, HttpCookie> getCookies(Headers headers) {
    List<String> cookies = headers.get(HttpHeaders.COOKIE);
    Map<String, HttpCookie> cookieMap = new HashMap<>();

    for (String cookieStr : cookies) {
      CookieParser parser = new CookieParser(cookieStr);
      for (Cookie c : parser) {
        HttpCookie httpCookie = new HttpCookie(c.getName(), c.getValue());
        cookieMap.put(httpCookie.getName(), httpCookie);
      }
    }

    return cookieMap;
  }

  /** Returns map of request parameters from the provided HttpExchange. */
  public static Map<String, String> getRequestParams(HttpExchange exchange) {
    URIBuilder builder = new URIBuilder(exchange.getRequestURI());
    List<NameValuePair> queryParamPairs = builder.getQueryParams();
    Map<String, String> params = new HashMap<String, String>();
    for (NameValuePair pair : queryParamPairs) {
      params.put(pair.getName(), pair.getValue());
    }
    return params;
  }

  public static String encodeId(UUID id) {
    Preconditions.checkNotNull(id);
    return BaseEncoding.base64Url().encode(id.toString().getBytes(Charsets.UTF_8));
  }

  public static UUID decodeId(String encoded) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encoded));
    return UUID.fromString(new String(BaseEncoding.base64Url().decode(encoded), Charsets.UTF_8));
  }

  /**
   * Returns whether or not the exchange is a valid request for the provided http method and
   * resource. If not, it will close the client connection.
   */
  public static boolean validateRequest(
      HttpExchange exchange, HttpMethods supportedMethod, String resourceRegex) throws IOException {
    String path = exchange.getRequestURI().getPath();
    if (!exchange.getRequestMethod().equalsIgnoreCase(supportedMethod.name())) {
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Method not supported\n".getBytes());
      writer.close();
      return false;
    } else if (!Pattern.compile(resourceRegex).matcher(path).matches()) {
      exchange.sendResponseHeaders(404, 0);
      OutputStream writer = exchange.getResponseBody();
      writer.write("Not found\n".getBytes());
      writer.close();
      return false;
    }
    return true;
  }

  /** Hack! For now, if we don't have export auth data, assume it's for export. */
  public static AuthMode getAuthMode(Headers headers) {
    String exportAuthCookie = getCookie(headers, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
    return (Strings.isNullOrEmpty(exportAuthCookie) ? AuthMode.EXPORT : AuthMode.IMPORT);
  }

  /**
   * Encrypts the given {@code authData} with the session-based {@link SecretKey} and stores it as a
   * cookie in the provided headers.
   */
  public static void setCookie(Headers headers, String encrypted, AuthMode authMode) {
    String cookieKey =
        (authMode == AuthMode.EXPORT)
            ? JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY
            : JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY;
    HttpCookie cookie = new HttpCookie(cookieKey, encrypted);
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString() + COOKIE_ATTRIBUTES);
  }

  /** Enumeration of http methods supported in the API reference implementation. */
  public enum HttpMethods {
    GET,
    POST
  }

  public static class FrontendConstantUrls {
    public static final String URL_NEXT_PAGE = "/next";
    public static final String URL_COPY_PAGE = "/copy";
  }
}
