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

import static com.google.common.base.Preconditions.checkState;

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
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.dataportabilityproject.transfer.smugmug.photos.model.ImageUploadResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumInfoResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugUser;
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
  private static final String BASE_URL = "https://api.smugmug.com";
  private static final String USER_URL = "/api/v2!authuser";
  private static final String ALBUMS_KEY = "UserAlbums";
  private static final String FOLDER_KEY = "Folder";

  private final OAuthService oAuthService;
  private final HttpTransport httpTransport;
  private final Token accessToken;
  private final ObjectMapper mapper;
  private final SmugMugUser user;

  SmugMugInterface(
      HttpTransport transport,
      AppCredentials appCredentials,
      TokenSecretAuthData authData,
      ObjectMapper mapper)
      throws IOException {
    this.httpTransport = transport;
    this.oAuthService =
        new ServiceBuilder()
            .apiKey(appCredentials.getKey())
            .apiSecret(appCredentials.getSecret())
            .provider(SmugMugOauthApi.class)
            .build();
    this.accessToken = new Token(authData.getToken(), authData.getSecret());
    this.mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.user = getUserInformation().getUser();
  }

  /* Returns the album information corresponding to the album URI provided. */
  SmugMugAlbumInfoResponse getAlbumInfo(String url) throws IOException {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(url), "Album URI is required to retrieve album information");
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumInfoResponse>>() {})
        .getResponse();
  }

  /* Returns the album corresponding to the url provided. If the url is null or empty, this
   * returns the top level user albums. */
  SmugMugAlbumsResponse getAlbums(String url) throws IOException {
    if (Strings.isNullOrEmpty(url)) {
      url = user.getUris().get(ALBUMS_KEY).getUri();
    }
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumsResponse>>() {})
        .getResponse();
  }

  /* Creates an album with albumName provided. */
  SmugMugAlbumResponse createAlbum(String albumName) throws IOException {
    // Set up album
    Map<String, String> json = new HashMap<>();
    String niceName = "Copy-of-" + albumName.replace(' ', '-');
    json.put("UrlName", niceName);
    // Allow conflicting names to be changed
    json.put("AutoRename", "true");
    json.put("Name", "Copy of " + albumName);
    // All imported content is private by default.
    json.put("Privacy", "Private");
    HttpContent content = new JsonHttpContent(new JacksonFactory(), json);

    // Upload album
    String folder = user.getUris().get(FOLDER_KEY).getUri();

    SmugMugResponse<SmugMugAlbumResponse> response =
        postRequest(
            folder + "!albums",
            content,
            ImmutableMap.of(),
            new TypeReference<SmugMugResponse<SmugMugAlbumResponse>>() {});

    checkState(response.getResponse() != null, "Response is null");
    checkState(response.getResponse().getAlbum() != null, "Album is null");

    return response.getResponse();
  }

  /* Uploads the resource at photoUrl to the albumId provided
   * The albumId must exist before calling upload, else the request will fail */
  ImageUploadResponse uploadImage(String photoUrl, String albumId) throws IOException {
    // Set up photo
    InputStreamContent content = new InputStreamContent(null, getImageAsStream(photoUrl));

    // Upload photo
    return postRequest(
        "http://upload.smugmug.com/",
        content,
        // Headers from: https://api.smugmug.com/api/v2/doc/reference/upload.html
        ImmutableMap.of(
            "X-Smug-AlbumUri", "/api/v2/album/" + albumId,
            "X-Smug-ResponseType", "json",
            "X-Smug-Version", "v2"),
        new TypeReference<ImageUploadResponse>() {});
  }

  private SmugMugUserResponse getUserInformation() throws IOException {
    return makeRequest(USER_URL, new TypeReference<SmugMugResponse<SmugMugUserResponse>>() {})
        .getResponse();
  }

  private InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  private <T> SmugMugResponse<T> makeRequest(
      String url, TypeReference<SmugMugResponse<T>> typeReference) throws IOException {
    OAuthRequest request =
        new OAuthRequest(Verb.GET, BASE_URL + url + "?_accept=application%2Fjson");
    oAuthService.signRequest(accessToken, request);
    final Response response = request.send();
    String result = response.getBody();
    // Note: there are no request params that need to go here, because smugmug fully specifies
    // which resource to get in the URL of a request, without using query params.
    return mapper.readValue(result, typeReference);
    // TODO: might want to check the response type here and or throw a smugmug exception similar to
    // what Flickr does
  }

  // TODO: move this to use scribe oauth service for signing, and make private.
  private <T> T postRequest(
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

    return mapper.readValue(result, typeReference);
  }
}
