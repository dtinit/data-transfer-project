/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.music;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemRequest;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemResponse;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylist;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistItemListResponse;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistListResponse;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;

/**
 * GoogleMusicHttpApi makes HTTP requests to read and write to the public Google Music's "Music
 * Library" APIs, following its documentation at
 * https://developers.google.com/youtube/mediaconnect.
 *
 * <p>Note that this is the lowest level of Google Music interaction - that is, you probably don't
 * want to use this class and are better off using something like a bit higher level like the Google
 * Music DTP Exporter and Importer instead.
 */
public class GoogleMusicHttpApi {

  private static final String BASE_URL =
      "https://youtubemediaconnect.googleapis.com/v1/users/me/musicLibrary/";
  private static final int PLAYLIST_PAGE_SIZE = 20;
  private static final int PLAYLIST_ITEM_PAGE_SIZE = 50;

  private static final String PAGE_SIZE_KEY = "pageSize";
  private static final String TOKEN_KEY = "pageToken";
  private static final String ORIGINAL_PLAYLIST_ID_KEY = "originalPlaylistId";
  private static final String ACCESS_TOKEN_KEY = "access_token";

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final HttpTransport httpTransport = new NetHttpTransport();
  private Credential credential;
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private final GoogleCredentialFactory credentialFactory;
  private final RateLimiter writeRateLimiter;

  GoogleMusicHttpApi(
      Credential credential,
      JsonFactory jsonFactory,
      Monitor monitor,
      GoogleCredentialFactory credentialFactory,
      double writesPerSecond) {
    this.credential = credential;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.writeRateLimiter = RateLimiter.create(writesPerSecond);
  }

  PlaylistListResponse listPlaylists(Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(PLAYLIST_PAGE_SIZE));
    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    return makeGetRequest(BASE_URL + "playlists", Optional.of(params), PlaylistListResponse.class);
  }

  PlaylistItemListResponse listPlaylistItems(String playlistId, Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(PLAYLIST_ITEM_PAGE_SIZE));
    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    return makeGetRequest(
        BASE_URL + "playlists/" + playlistId + "/playlistItems", Optional.of(params),
        PlaylistItemListResponse.class);
  }

  GooglePlaylist createPlaylist(GooglePlaylist playlist, String playlistId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, Object> playlistMap = createJsonMap(playlist);
    Map<String, String> params = new LinkedHashMap<>();
    params.put(ORIGINAL_PLAYLIST_ID_KEY, playlistId);
    HttpContent content = new JsonHttpContent(jsonFactory, playlistMap);

    return makePatchRequest(BASE_URL + "playlists/" + playlistId ,Optional.of(params), content,
        GooglePlaylist.class);
  }

  BatchPlaylistItemResponse createPlaylistItems(BatchPlaylistItemRequest playlistItemRequest)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, Object> playlistItemMap = createJsonMap(playlistItemRequest);
    HttpContent content = new JsonHttpContent(jsonFactory, playlistItemMap);

    return makePostRequest(
        BASE_URL + "playlistItems:batchCreate", Optional.empty(), content,
        BatchPlaylistItemResponse.class);
  }

  private <T> T makeGetRequest(
      String baseUrl, Optional<Map<String, String>> parameters, Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(baseUrl + "?" + generateParamsString(parameters)));

    HttpResponse response;
    try {
      response = getRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildGetRequest(
                      new GenericUrl(baseUrl + "?" + generateParamsString(parameters))),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result = CharStreams.toString(new InputStreamReader(response.getContent(), UTF_8));
    Gson gson = new Gson();
    return gson.fromJson(result, clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> T makePostRequest(
      String baseUrl,
      Optional<Map<String, String>> parameters,
      HttpContent httpContent,
      Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // Wait for write permit before making request
    writeRateLimiter.acquire();

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(baseUrl + "?" + generateParamsString(parameters)), httpContent);
    postRequest.setReadTimeout(2 * 60000); // 2 minutes read timeout
    HttpResponse response;

    try {
      response = postRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildPostRequest(
                      new GenericUrl(baseUrl + "?" + generateParamsString(parameters)),
                      httpContent),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result = CharStreams.toString(new InputStreamReader(response.getContent(), UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T makePatchRequest(
      String baseUrl,
      Optional<Map<String, String>> parameters,
      HttpContent httpContent,
      Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // Wait for write permit before making request
    writeRateLimiter.acquire();

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
    HttpRequest patchRequest =
        requestFactory.buildPatchRequest(
                new GenericUrl(baseUrl + "?" + generateParamsString(parameters)), httpContent)
            .setRequestMethod("POST")
            .setHeaders(new HttpHeaders().set("X-HTTP-Method-Override", "PATCH"));
    ;
    patchRequest.setReadTimeout(2 * 60000); // 2 minutes read timeout
    HttpResponse response;

    try {
      response = patchRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildPatchRequest(
                          new GenericUrl(baseUrl + "?" + generateParamsString(parameters)),
                          httpContent).setRequestMethod("POST")
                      .setHeaders(new HttpHeaders().set("X-HTTP-Method-Override", "PATCH")),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result = CharStreams.toString(new InputStreamReader(response.getContent(), UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  private HttpResponse handleHttpResponseException(
      SupplierWithIO<HttpRequest> httpRequest, HttpResponseException e)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // if the response is "unauthorized", refresh the token and try the request again
    final int statusCode = e.getStatusCode();

    if (statusCode == 401) {
      monitor.info(() -> "Attempting to refresh authorization token");
      // if the credential refresh failed, let the error bubble up via the IOException that gets
      // thrown
      credential = credentialFactory.refreshCredential(credential);
      monitor.info(() -> "Refreshed authorization token successfully");

      // if the second attempt throws an error, then something else is wrong, and we bubble up the
      // response errors
      return httpRequest.getWithIO().execute();
    }

    if (statusCode == 403) {
      throw new PermissionDeniedException("User permission to youtube music was denied", e);
    } else {
      // something else is wrong, bubble up the error
      throw new IOException(
          "Bad status code: "
              + e.getStatusCode()
              + " Error: '"
              + e.getStatusMessage()
              + "' Content: "
              + e.getContent());
    }
  }

  private String generateParamsString(Optional<Map<String, String>> params) {
    Map<String, String> updatedParams = new ArrayMap<>();
    if (params.isPresent()) {
      updatedParams.putAll(params.get());
    }

    updatedParams.put(ACCESS_TOKEN_KEY, Preconditions.checkNotNull(credential.getAccessToken()));

    List<String> orderedKeys =
        updatedParams.keySet().stream().collect(toCollection(ArrayList::new));
    Collections.sort(orderedKeys);

    List<String> paramStrings = new ArrayList<>();
    for (String key : orderedKeys) {
      String k = key.trim();
      String v = updatedParams.get(key).trim();

      paramStrings.add(k + "=" + v);
    }

    return String.join("&", paramStrings);
  }

  private HashMap<String, Object> createJsonMap(Object object) throws IOException {
    // JacksonFactory expects to receive a Map, not a JSON-annotated POJO, so we have to convert the
    // NewMediaItemUpload to a Map before making the HttpContent.
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {
        };
    return objectMapper.readValue(objectMapper.writeValueAsString(object), typeRef);
  }

  private interface SupplierWithIO<T> {

    T getWithIO() throws IOException;
  }
}
