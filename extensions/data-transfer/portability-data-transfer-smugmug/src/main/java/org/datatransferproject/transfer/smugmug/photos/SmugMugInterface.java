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
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.google.api.client.http.InputStreamContent;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.datatransferproject.transfer.smugmug.photos.model.*;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumImageResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugImageUploadResponse;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class SmugMugInterface {

  private static final String BASE_URL = "https://api.smugmug.com";
  private static final String USER_URL = "/api/v2!authuser";
  private static final String ALBUMS_KEY = "UserAlbums";
  private static final String FOLDER_KEY = "Folder";

  private final OAuth10aService oAuthService;
  private final OAuth1AccessToken accessToken;
  private final ObjectMapper mapper;
  private final SmugMugUser user;

  SmugMugInterface(AppCredentials appCredentials, TokenSecretAuthData authData, ObjectMapper mapper)
      throws IOException {
    this.oAuthService =
        new ServiceBuilder(appCredentials.getKey())
            .apiSecret(appCredentials.getSecret())
            .build(new SmugMugOauthApi());
    this.accessToken = new OAuth1AccessToken(authData.getToken(), authData.getSecret());
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
    json.put("Title", albumName);
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
      headersMap.put("X-Smug-Title", cleanHeader(photoModel.getTitle()));
    }
    if (!Strings.isNullOrEmpty(photoModel.getDescription())) {
      headersMap.put("X-Smug-Caption", cleanHeader(photoModel.getDescription()));
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

  public InputStream getImageAsStream(String urlStr) throws IOException {
    OAuthRequest request = new OAuthRequest(Verb.GET, urlStr);
    oAuthService.signRequest(accessToken, request);
    try {
      final Response response = oAuthService.execute(request);
      return response.getStream();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
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

    fullUrl += (url.contains("?") ? "&" : "?") + "_accept=application%2Fjson";

    OAuthRequest request = new OAuthRequest(Verb.GET, fullUrl);
    oAuthService.signRequest(accessToken, request);
    try {
      final Response response = oAuthService.execute(request);
      if (response.getCode() < 200 || response.getCode() >= 300) {
        throw new IOException(
            String.format("Error occurred in request for %s : %s", url, response.getMessage()));
      }
      return mapper.readValue(response.getBody(), typeReference);
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
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
      request.setPayload(contentBytes);
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

    try {
      Response response = oAuthService.execute(request);
      if (response.getCode() < 200 || response.getCode() >= 300) {
        if (response.getCode() == 400) {
          throw new IOException(
              String.format(
                  "Error occurred in request for %s, code: %s, message: %s, request: %s,"
                      + " bodyParams: %s, payload: %s",
                  fullUrl,
                  response.getCode(),
                  response.getMessage(),
                  request,
                  request.getBodyParams(),
                  new String(request.getByteArrayPayload(), request.getCharset())));
        }
        throw new IOException(
            String.format(
                "Error occurred in request for %s, code: %s, message: %s",
                fullUrl, response.getCode(), response.getMessage()));
      }
      return mapper.readValue(response.getBody(), typeReference);
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
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

  static String cleanHeader(String header) {
    return header.replace("\n", "%0A").replace("\r", "%0D");
  }
}
