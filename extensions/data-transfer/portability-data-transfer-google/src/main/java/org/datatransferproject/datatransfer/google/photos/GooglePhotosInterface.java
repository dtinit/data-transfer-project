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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.google.photos.model.AlbumListResponse;
import org.datatransferproject.datatransfer.google.photos.model.MediaItemSearchResponse;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GooglePhotosInterface {

  private static final String BASE_URL = "https://photoslibrary.googleapis.com/v1/";
  private static final int ALBUM_PAGE_SIZE = 20; // TODO
  private static final int MEDIA_PAGE_SIZE = 100; // TODO

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpTransport httpTransport = new NetHttpTransport();
  private final Credential credential;

  public GooglePhotosInterface(Credential credential) {
    this.credential = credential;
  }

  public AlbumListResponse listAlbums(Optional<String> pageToken) throws IOException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("pageSize", String.valueOf(ALBUM_PAGE_SIZE));
    if (pageToken.isPresent()) {
      params.put("pageToken", pageToken.get());
    }
    return makeGetRequest(BASE_URL + "albums", Optional.of(params),
        AlbumListResponse.class);
  }

  public MediaItemSearchResponse listAlbumContents(String albumId, Optional<String> pageToken)
      throws IOException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("pageSize", String.valueOf(MEDIA_PAGE_SIZE));
    params.put("albumId", albumId);
    if (pageToken.isPresent()) {
      params.put("pageToken", pageToken.get());
    }
    HttpContent content = new JsonHttpContent(new JacksonFactory(), params);
    return makePostRequest(BASE_URL + "mediaItems:search", Optional.empty(), content,
        MediaItemSearchResponse.class);
  }

  private <T> T makeGetRequest(String url, Optional<Map<String, String>> parameters, Class<T> clazz)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory
        .buildGetRequest(new GenericUrl(url + "?" + generateParamsString(parameters)));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result = CharStreams
        .toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  private <T> T makePostRequest(String url, Optional<Map<String, String>> parameters,
      HttpContent httpContent, Class<T> clazz)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory
        .buildPostRequest(new GenericUrl(url + "?" + generateParamsString(parameters)),
            httpContent);
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result = CharStreams
        .toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  private String generateParamsString(Optional<Map<String, String>> params) {
    Map<String, String> updatedParams = new ArrayMap<>();
    if (params.isPresent()) {
      updatedParams.putAll(params.get());
    }
    if (!updatedParams.containsKey("access_token")) {
      updatedParams.put("access_token", credential.getAccessToken());
    }

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
}
