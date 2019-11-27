/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.videos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
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
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;

public class GoogleVideosInterface {

  private static final int ALBUM_PAGE_SIZE = 20; // TODO
  private static final int MEDIA_PAGE_SIZE = 50; // TODO

  private static final String PAGE_SIZE_KEY = "pageSize";
  private static final String TOKEN_KEY = "pageToken";
  private static final String ALBUM_ID_KEY = "albumId";
  private static final String ACCESS_TOKEN_KEY = "access_token";
  private static final String FILTERS_KEY = "filters";
  private static final String INCLUDE_ARCHIVED_KEY = "includeArchivedMedia";
  private static final String MEDIA_FILTER_KEY = "mediaTypeFilter";
  private static final String BASE_URL = "https://photoslibrary.googleapis.com/v1/";

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final HttpTransport httpTransport = new NetHttpTransport();
  private final Credential credential;
  private JsonFactory jsonFactory;

  GoogleVideosInterface(Credential credential, JsonFactory jsonFactory) {
    this.credential = credential;
    this.jsonFactory = jsonFactory;
  }

  String uploadVideoContent(InputStream inputStream, String filename) throws IOException {
    InputStreamContent content = new InputStreamContent(null, inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    byte[] contentBytes = outputStream.toByteArray();
    HttpContent httpContent = new ByteArrayContent(null, contentBytes);

    return makePostRequest(BASE_URL + "uploads/", Optional.empty(), httpContent, String.class);
  }

  BatchMediaItemResponse createVideo(NewMediaItemUpload newMediaItemUpload) throws IOException {
    HashMap<String, Object> map = createJsonMap(newMediaItemUpload);
    HttpContent httpContent = new JsonHttpContent(this.jsonFactory, map);

    return makePostRequest(
        BASE_URL + "mediaItems:batchCreate",
        Optional.empty(),
        httpContent,
        BatchMediaItemResponse.class);
  }

  MediaItemSearchResponse listVideoItems(Optional<String> pageToken) throws IOException {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(MEDIA_PAGE_SIZE));

    params.put(
        FILTERS_KEY,
        ImmutableMap.of(
            MEDIA_FILTER_KEY, ImmutableMap.of("mediaTypes", ImmutableList.of("VIDEO"))));

    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    HttpContent content = new JsonHttpContent(this.jsonFactory, params);
    return makePostRequest(
        BASE_URL + "mediaItems:search", Optional.empty(), content, MediaItemSearchResponse.class);
  }

  <T> T makePostRequest(
      String url, Optional<Map<String, String>> parameters, HttpContent httpContent, Class<T> clazz)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent);

    // TODO: Figure out why this is necessary for videos but not for photos
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType("application/octet-stream");
    headers.setAuthorization("Bearer " + this.credential.getAccessToken());
    headers.set("X-Goog-Upload-Protocol", "raw");
    postRequest.setHeaders(headers);

    HttpResponse response = postRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  private String generateParamsString(Optional<Map<String, String>> params) throws IOException {
    Map<String, String> updatedParams = new ArrayMap<>();
    if (params.isPresent()) {
      updatedParams.putAll(params.get());
    }

    // getAccessToken will return null when the token needs to be refreshed
    if (credential.getAccessToken() == null) {
      credential.refreshToken();
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
}
