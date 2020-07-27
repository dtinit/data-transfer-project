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

package org.datatransferproject.datatransfer.google.photos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.RateLimiter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;

public class GooglePhotosInterface {

  private static final String BASE_URL = "https://photoslibrary.googleapis.com/v1/";
  private static final int ALBUM_PAGE_SIZE = 20; // TODO
  private static final int MEDIA_PAGE_SIZE = 50; // TODO

  private static final String PAGE_SIZE_KEY = "pageSize";
  private static final String TOKEN_KEY = "pageToken";
  private static final String ALBUM_ID_KEY = "albumId";
  private static final String ACCESS_TOKEN_KEY = "access_token";
  private static final String FILTERS_KEY = "filters";
  private static final String INCLUDE_ARCHIVED_KEY = "includeArchivedMedia";
  private static final Map<String, String> PHOTO_UPLOAD_PARAMS =
      ImmutableMap.of(
          "Content-type", "application/octet-stream",
          "X-Goog-Upload-Protocol", "raw");

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final HttpTransport httpTransport = new NetHttpTransport();
  private Credential credential;
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private final GoogleCredentialFactory credentialFactory;
  private final RateLimiter writeRateLimiter;

  GooglePhotosInterface(
      GoogleCredentialFactory credentialFactory,
      Credential credential,
      JsonFactory jsonFactory,
      Monitor monitor,
      double writesPerSecond) {
    this.credential = credential;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    writeRateLimiter = RateLimiter.create(writesPerSecond);
  }

  AlbumListResponse listAlbums(Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(ALBUM_PAGE_SIZE));
    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    return makeGetRequest(BASE_URL + "albums", Optional.of(params), AlbumListResponse.class);
  }

  GoogleAlbum getAlbum(String albumId) throws IOException, InvalidTokenException, PermissionDeniedException{
    Map<String, String> params = new LinkedHashMap<>();
    return makeGetRequest(BASE_URL + "albums/" + albumId, Optional.of(params), GoogleAlbum.class);
  }

  GoogleMediaItem getMediaItem(String mediaId) throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    return makeGetRequest(BASE_URL + "mediaItems/" + mediaId, Optional.of(params), GoogleMediaItem
        .class);
  }

  MediaItemSearchResponse listMediaItems(Optional<String> albumId, Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(MEDIA_PAGE_SIZE));
    if (albumId.isPresent()) {
      params.put(ALBUM_ID_KEY, albumId.get());
    } else {
      params.put(FILTERS_KEY, ImmutableMap.of(INCLUDE_ARCHIVED_KEY, String.valueOf(true)));
    }
    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    HttpContent content = new JsonHttpContent(new JacksonFactory(), params);
    return makePostRequest(
        BASE_URL + "mediaItems:search", Optional.empty(), content, MediaItemSearchResponse.class);
  }

  GoogleAlbum createAlbum(GoogleAlbum googleAlbum)
          throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, Object> albumMap = createJsonMap(googleAlbum);
    Map<String, Object> contentMap = ImmutableMap.of("album", albumMap);
    HttpContent content = new JsonHttpContent(jsonFactory, contentMap);

    return makePostRequest(BASE_URL + "albums", Optional.empty(), content, GoogleAlbum.class);
  }

  String uploadPhotoContent(InputStream inputStream)
          throws IOException, InvalidTokenException, PermissionDeniedException {
    // TODO: add filename
    InputStreamContent content = new InputStreamContent(null, inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    byte[] contentBytes = outputStream.toByteArray();
    if (contentBytes.length == 0) {
      // Google Photos cannot add an empty photo so gracefully ignore
      return "EMPTY_PHOTO";
    }
    HttpContent httpContent = new ByteArrayContent(null, contentBytes);

    return makePostRequest(
        BASE_URL + "uploads/", Optional.of(PHOTO_UPLOAD_PARAMS), httpContent, String.class);
  }

  BatchMediaItemResponse createPhotos(NewMediaItemUpload newMediaItemUpload)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    HashMap<String, Object> map = createJsonMap(newMediaItemUpload);
    HttpContent httpContent = new JsonHttpContent(new JacksonFactory(), map);

    return makePostRequest(
        BASE_URL + "mediaItems:batchCreate",
        Optional.empty(),
        httpContent,
        BatchMediaItemResponse.class);
  }

  private <T> T makeGetRequest(String url, Optional<Map<String, String>> parameters, Class<T> clazz)
          throws IOException, InvalidTokenException, PermissionDeniedException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)));

    HttpResponse response;
    try {
      response = getRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildGetRequest(
                      new GenericUrl(url + "?" + generateParamsString(parameters))),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  <T> T makePostRequest(
      String url, Optional<Map<String, String>> parameters, HttpContent httpContent, Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // Wait for write permit before making request
    writeRateLimiter.acquire();

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent);
    postRequest.setReadTimeout(2 * 60000); // 2 minutes read timeout
    HttpResponse response;

    try {
      response = postRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildPostRequest(
                      new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
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
      monitor.info(() -> "Refreshed authorization token successfuly");

      // if the second attempt throws an error, then something else is wrong, and we bubble up the
      // response errors
      return httpRequest.getWithIO().execute();
    }
    // "The caller does not have permission" is potential error for albums.
    // "Google Photos is disabled for the user" is potential error for photos.
    if (statusCode == 403 &&
            (e.getContent().contains("The caller does not have permission") ||
             e.getContent().contains("Google Photos is disabled for the user"))) {
      throw new PermissionDeniedException("User permission to google photos was denied", e);
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

    List<String> orderedKeys = updatedParams.keySet().stream().collect(Collectors.toList());
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
        new TypeReference<HashMap<String, Object>>() {};
    return objectMapper.readValue(objectMapper.writeValueAsString(object), typeRef);
  }

  private interface SupplierWithIO<T> {
    T getWithIO() throws IOException;
  }
}
