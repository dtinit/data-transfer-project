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

package org.dataportabilityproject.transfer.smugmug.photos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumInfoResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugUserResponse;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class SmugMugInterface {

  // TODO(olsona): is this the mapper we want to use?
  static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  static final String BASE_URL = "https://api.smugmug.com";
  static final String USER_URL = "/api/v2!authuser";
  static final String ALBUMS_KEY = "UserAlbums";
  private final OAuthService oAuthService;

  private final HttpTransport httpTransport;
  private final Token accessToken;

  SmugMugInterface(
      HttpTransport transport, AppCredentials appCredentials, TokenSecretAuthData authData) {
    this.httpTransport = transport;
    this.oAuthService =
        new ServiceBuilder()
            .apiKey(appCredentials.getKey())
            .apiSecret(appCredentials.getSecret())
            .provider(SmugMugOauthApi.class)
            .build();
    this.accessToken = new Token(authData.getToken(), authData.getSecret());
  }

  SmugMugResponse<SmugMugAlbumInfoResponse> makeAlbumInfoRequest(String url) throws IOException {
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumInfoResponse>>() {});
  }

  SmugMugResponse<SmugMugAlbumsResponse> makeAlbumRequest(String url) throws IOException {
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumsResponse>>() {});
  }

  SmugMugResponse<SmugMugUserResponse> makeUserRequest() throws IOException {
    return makeRequest(USER_URL, new TypeReference<SmugMugResponse<SmugMugUserResponse>>() {});
  }

  <T> SmugMugResponse<T> makeRequest(
      String url, TypeReference<SmugMugResponse<T>> typeReference)
      throws IOException {
    OAuthRequest request = new OAuthRequest(Verb.GET, BASE_URL + url + "?_accept=application%2Fjson");
    oAuthService.signRequest(accessToken, request);
    final Response response = request.send();
    String result = response.getBody();
    // Note: there are no request params that need to go here, because smugmug fully specifies
    // which resource to get in the URL of a request, without using query params.
    return MAPPER.readValue(result, typeReference);
  }

  // TODO: move this to use scribe oauth service for signing.
  <T> T postRequest(
      String url, HttpContent content, Map<String, String> headers, TypeReference<T> typeReference)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + url;
    }

    HttpRequest postRequest = requestFactory.buildPostRequest(new GenericUrl(fullUrl), content);
    HttpHeaders httpHeaders =
        new HttpHeaders().setAccept("application/json").setContentType("application/json");
    for (Entry<String, String> entry : headers.entrySet()) {
      httpHeaders.put(entry.getKey(), entry.getValue());
    }
    postRequest.setHeaders(httpHeaders);

    // TODO(olsona): sign request

    HttpResponse response;
    try {
      response = postRequest.execute();
    } catch (HttpResponseException e) {
      throw new IOException("Problem making request: " + postRequest.getUrl(), e);
    }
    int statusCode = response.getStatusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));

    return MAPPER.readValue(result, typeReference);
  }
}
