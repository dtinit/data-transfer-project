/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.instagram.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.transfer.instagram.model.ExchangeTokenResponse;
import org.datatransferproject.transfer.instagram.model.MediaResponse;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class InstagramApiClient {
  private static final String MEDIA_BASE_URL =
      "https://graph.instagram.com/me/media?fields=id,media_url,media_type,caption,timestamp,children%7Bid,media_url,media_type%7D";
  private static final String ACCESS_TOKEN_BASE_URL =
      "https://graph.instagram.com/access_token?grant_type=ig_exchange_token";

  private final ObjectMapper objectMapper;
  private final HttpTransport httpTransport;
  private final Monitor monitor;
  private final AppCredentials appCredentials;
  private final String accessToken;

  public InstagramApiClient(
      HttpTransport httpTransport,
      Monitor monitor,
      AppCredentials appCredentials,
      TokensAndUrlAuthData authData)
      throws IOException {
    this.objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.httpTransport = httpTransport;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.accessToken = getLongLivedAccessToken(authData);
  }

  public MediaResponse makeRequest(String url) throws IOException {
    return makeRequest(url, MediaResponse.class, this.accessToken);
  }

  public static String getMediaBaseUrl() {
    return MEDIA_BASE_URL;
  }

  public static String getContinuationUrl(MediaResponse response) throws IOException {
    String next = response.getPaging().getNext();
    if (next != null && !next.isEmpty()) {
      try {
        String after = response.getPaging().getCursors().getAfter();
        return new URIBuilder(MEDIA_BASE_URL).setParameter("after", after).build().toString();
      } catch (URISyntaxException e) {
        throw new IOException("Failed to produce instagram paging url.", e);
      }
    }
    return null;
  }

  private String getLongLivedAccessToken(TokensAndUrlAuthData authData) throws IOException {
    try {
      String url =
          new URIBuilder(ACCESS_TOKEN_BASE_URL)
              .setParameter("client_secret", this.appCredentials.getSecret())
              .build()
              .toString();
      ExchangeTokenResponse response =
          makeRequest(url, ExchangeTokenResponse.class, authData.getAccessToken());
      return response.getAccessToken();
    } catch (URISyntaxException e) {
      throw new IOException("Failed to produce instagram long lived token exchange url.", e);
    }
  }

  private <T> T makeRequest(String url, Class<T> clazz, String accessToken) throws IOException {
    String fullURL;
    try {
      fullURL = new URIBuilder(url).setParameter("access_token", accessToken).build().toString();
    } catch (URISyntaxException e) {
      throw new IOException("Failed to produce instagram api url.", e);
    }

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(fullURL));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }
}
