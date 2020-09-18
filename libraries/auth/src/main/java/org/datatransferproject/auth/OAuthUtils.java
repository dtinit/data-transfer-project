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

package org.datatransferproject.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Util class for making OAuth requests and parsing the responses
 */
class OAuthUtils {

  static String makeRawPostRequest(HttpTransport httpTransport, String url, HttpContent httpContent)
      throws IOException {
    HttpRequestFactory factory = httpTransport.createRequestFactory();
    HttpRequest postRequest = factory.buildPostRequest(new GenericUrl(url), httpContent);
    HttpResponse response = postRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    return CharStreams.toString(new InputStreamReader(response.getContent(), UTF_8));
  }

  static <T> T makePostRequest(HttpTransport httpTransport, String url, HttpContent httpContent,
      Class<T> clazz) throws IOException {
    String result = makeRawPostRequest(httpTransport, url, httpContent);

    return new ObjectMapper().readValue(result, clazz);
  }
}