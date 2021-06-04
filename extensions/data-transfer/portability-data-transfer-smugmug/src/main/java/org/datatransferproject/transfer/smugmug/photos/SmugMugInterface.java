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

package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.InputStreamContent;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import org.datatransferproject.transfer.smugmug.photos.model.*;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SmugMugInterface {

  private static final String BASE_URL = "https://api.smugmug.com";
  private static final String USER_URL = "/api/v2!authuser";
  private static final String ALBUMS_KEY = "UserAlbums";
  private static final String FOLDER_KEY = "Folder";

  private final OAuthService oAuthService;
  private final Token accessToken;
  private final ObjectMapper mapper;
  private final SmugMugUser user;

  SmugMugInterface(AppCredentials appCredentials, TokenSecretAuthData authData, ObjectMapper mapper)
      throws IOException {
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

  SmugMugAlbumImageResponse getListOfAlbumImages(String url) throws IOException {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(url), "Album URI is required to retrieve album information");
    SmugMugAlbumImageResponse response =
        makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumImageResponse>>() {})
            .getResponse();
    return response;
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
    json.put("NiceName", cleanName(albumName));
    // Allow conflicting names to be changed
    json.put("AutoRename", "true");
    json.put("Title", "Copy of " + albumName);
    // All imported content is private by default.
    json.put("Privacy", "Private");

    // Upload album
    String folder = user.getUris().get(FOLDER_KEY).getUri();

    SmugMugResponse<SmugMugAlbumResponse> response =
        postRequest(
            folder + "!albums",
            json,
            null, // No HttpContent for album creation
            ImmutableMap.of(), // No special Smugmug headers are required
            new TypeReference<SmugMugResponse<SmugMugAlbumResponse>>() {});

    Preconditions.checkState(response.getResponse() != null, "Response is null");
    Preconditions.checkState(response.getResponse().getAlbum() != null, "Album is null");

    return response.getResponse();
  }

  /* Uploads the resource at photoUrl to the albumId provided
   * The albumId must exist before calling upload, else the request will fail */
  SmugMugImageUploadResponse uploadImage(
      PhotoModel photoModel, String albumUri, InputStream inputStream) throws IOException {
    // Set up photo
    InputStreamContent content = new InputStreamContent(null, inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    byte[] contentBytes = outputStream.toByteArray();

    // Headers from: https://api.smugmug.com/api/v2/doc/reference/upload.html
    Map<String, String> headersMap = new HashMap<>();
    headersMap.put("X-Smug-AlbumUri", albumUri);
    headersMap.put("X-Smug-ResponseType", "JSON");
    headersMap.put("X-Smug-Version", "v2");
    headersMap.put("Content-Type", photoModel.getMediaType());

    if (!Strings.isNullOrEmpty(photoModel.getTitle())) {
      headersMap.put("X-Smug-Title", photoModel.getTitle());
    }
    if (!Strings.isNullOrEmpty(photoModel.getDescription())) {
      headersMap.put("X-Smug-Caption", photoModel.getDescription());
    }

    // Upload photo
    SmugMugImageUploadResponse response =
        postRequest(
            "https://upload.smugmug.com/",
            ImmutableMap.of(), // No content params for photo upload
            contentBytes,
            headersMap,
            new TypeReference<SmugMugImageUploadResponse>() {});

    Preconditions.checkState(response.getStat().equals("ok"), "Failed to upload image");
    return Preconditions.checkNotNull(response, "Image upload Response is null");
  }

  private SmugMugUserResponse getUserInformation() throws IOException {
    return makeRequest(USER_URL, new TypeReference<SmugMugResponse<SmugMugUserResponse>>() {})
        .getResponse();
  }

  public InputStream getImageAsStream(String urlStr) {
    OAuthRequest request = new OAuthRequest(Verb.GET, urlStr);
    oAuthService.signRequest(accessToken, request);
    final Response response = request.send();
    return response.getStream();
  }

  private <T> SmugMugResponse<T> makeRequest(
      String url, TypeReference<SmugMugResponse<T>> typeReference) throws IOException {
    // Note: there are no request params that need to go here, because smugmug fully specifies
    // which resource to get in the URL of a request, without using query params.
    String fullUrl;
    if (!url.contains("https://")) {
      fullUrl = BASE_URL + url;
    } else {
      fullUrl = url;
    }
    OAuthRequest request = new OAuthRequest(Verb.GET, fullUrl + "?_accept=application%2Fjson");
    oAuthService.signRequest(accessToken, request);
    final Response response = request.send();

    if (response.getCode() < 200 || response.getCode() >= 300) {
      throw new IOException(
          String.format("Error occurred in request for %s : %s", url, response.getMessage()));
    }

    return mapper.readValue(response.getBody(), typeReference);
  }

  // Makes a post request with the content parameters provided as the body, or the httpcontent as
  // the body
  private <T> T postRequest(
      String url,
      Map<String, String> contentParams,
      @Nullable byte[] contentBytes,
      Map<String, String> smugMugHeaders,
      TypeReference<T> typeReference)
      throws IOException {

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + url;
    }
    OAuthRequest request = new OAuthRequest(Verb.POST, fullUrl);

    // Add payload
    if (contentBytes != null) {
      request.addPayload(contentBytes);
    }

    // Add body params
    for (Entry<String, String> param : contentParams.entrySet()) {
      request.addBodyParameter(param.getKey(), param.getValue());
    }

    // sign request before adding any of the headers since those shouldn't be included in the
    // signature
    oAuthService.signRequest(accessToken, request);

    // Add headers
    for (Entry<String, String> header : smugMugHeaders.entrySet()) {
      request.addHeader(header.getKey(), header.getValue());
    }
    // add accept and content type headers so the response comes back in json and not html
    request.addHeader(HttpHeaders.ACCEPT, "application/json");

    Response response = request.send();
    if (response.getCode() < 200 || response.getCode() >= 300) {
      if (response.getCode() == 400) {
        throw new IOException(
            String.format(
                "Error occurred in request for %s, code: %s, message: %s, request: %s, bodyParams: %s, payload: %s",
                fullUrl,
                response.getCode(),
                response.getMessage(),
                request,
                request.getBodyParams(),
                request.getBodyContents()));
      }
      throw new IOException(
          String.format(
              "Error occurred in request for %s, code: %s, message: %s",
              fullUrl, response.getCode(), response.getMessage()));
    }
    return mapper.readValue(response.getBody(), typeReference);
  }

  static String cleanName(String name) {
    // TODO:  Handle cases where the entire album name is non-alphanumeric, e.g. all emojis
    return name.chars()
        .mapToObj(c -> (char) c)
        .map(c -> Character.isWhitespace(c) ? '-' : c)
        .filter(c -> Character.isLetterOrDigit(c) || c == '-')
        .limit(40)
        .map(Object::toString)
        .collect(Collectors.joining(""));
  }
}
