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
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.RateLimiter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.datatransferproject.spi.transfer.types.UploadErrorException;

// TODO (#1307): Find a way to consolidate all 3P API interfaces
public class GooglePhotosInterface {
  public static final String ERROR_HASH_MISMATCH = "Hash mismatch";
  private static final String GOOG_ERROR_HASH_MISMATCH_LEGACY = "Checksum from header does not match received payload content.";
  private static final String GOOG_ERROR_HASH_MISMATCH_UNIFIED = "User-provided checksum does not match received payload content.";

  private static final String GOOGPHOTOS_ALBUMS_PERMISSION_ERROR = "The caller does not have permission";
  private static final String GOOGPHOTOS_PHOTO_PERMISSION_ERROR = "Google Photos is disabled for the user";

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

  public GooglePhotosInterface(
      GoogleCredentialFactory credentialFactory,
      Credential credential,
      JsonFactory jsonFactory,
      Monitor monitor,
      double writesPerSecond) {
    this.credentialFactory = credentialFactory;
    this.credential = credential;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
    writeRateLimiter = RateLimiter.create(writesPerSecond);
  }

  public AlbumListResponse listAlbums(Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(ALBUM_PAGE_SIZE));
    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    return makeGetRequest(BASE_URL + "albums", Optional.of(params), AlbumListResponse.class);
  }

  public GoogleAlbum getAlbum(String albumId) throws IOException, InvalidTokenException, PermissionDeniedException{
    Map<String, String> params = new LinkedHashMap<>();
    return makeGetRequest(BASE_URL + "albums/" + albumId, Optional.of(params), GoogleAlbum.class);
  }

  public GoogleMediaItem getMediaItem(String mediaId) throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, String> params = new LinkedHashMap<>();
    return makeGetRequest(BASE_URL + "mediaItems/" + mediaId, Optional.of(params), GoogleMediaItem
        .class);
  }

  public MediaItemSearchResponse listMediaItems(Optional<String> albumId, Optional<String> pageToken)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
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
    HttpContent content = new JsonHttpContent(this.jsonFactory, params);
    return makePostRequest(BASE_URL + "mediaItems:search", Optional.empty(), Optional.empty(),
        content, MediaItemSearchResponse.class);
  }

  public GoogleAlbum createAlbum(GoogleAlbum googleAlbum)
          throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    Map<String, Object> albumMap = createJsonMap(googleAlbum);
    Map<String, Object> contentMap = ImmutableMap.of("album", albumMap);
    HttpContent content = new JsonHttpContent(jsonFactory, contentMap);

    return makePostRequest(BASE_URL + "albums", Optional.empty(), Optional.empty(), content,
        GoogleAlbum.class);
  }

  public String uploadMediaContent(InputStream inputStream, @Nullable String sha1)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
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

    // Adding optional fields.
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    if (sha1 != null && !sha1.isEmpty()) {
      // Running a very naive pre-check on the string format.
      Preconditions.checkState(sha1.length() == 40, "Invalid SHA-1 string.");
      // Note that the base16 encoder only accepts upper cases.
      headers.put("X-Goog-Hash", "sha1=" + Base64.getEncoder()
          .encodeToString(BaseEncoding.base16().decode(sha1.toUpperCase())));
    }

    return makePostRequest(BASE_URL + "uploads/", Optional.of(PHOTO_UPLOAD_PARAMS),
        Optional.of(headers.build()), httpContent, String.class);
  }

  public BatchMediaItemResponse createPhotos(NewMediaItemUpload newMediaItemUpload)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    HashMap<String, Object> map = createJsonMap(newMediaItemUpload);
    HttpContent httpContent = new JsonHttpContent(this.jsonFactory, map);

    return makePostRequest(BASE_URL + "mediaItems:batchCreate", Optional.empty(), Optional.empty(),
        httpContent, BatchMediaItemResponse.class);
  }

  private <T> T makeGetRequest(String url, Optional<Map<String, String>> parameters, Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

    HttpResponse response;
    try {
      response = makeHttpRequest(() ->
                  requestFactory.buildGetRequest(
                      new GenericUrl(url + "?" + generateParamsString(parameters))));
    } catch (UploadErrorException e) {
      throw new IllegalStateException("GET request unexpectedly produced Upload exception", e);
    }

    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), StandardCharsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  public <T> T makePostRequest(
      String url,
      Optional<Map<String, String>> parameters,
      Optional<Map<String, String>> extraHeaders,
      HttpContent httpContent,
      Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpResponse response = makeHttpRequest(() -> {
      // Wait for write permit before making request
      writeRateLimiter.acquire();

      HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent);
      extraHeaders.ifPresent(stringStringMap -> stringStringMap.forEach(
            (key, value) -> postRequest.getHeaders().set(key, value)));
      postRequest.setReadTimeout(2 * 60000); // 2 minutes read timeout
      return postRequest;
    });

    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), StandardCharsets.UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  private HttpResponse makeHttpRequest(SupplierWithIO<HttpRequest> httpRequest)
  throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {

    HttpResponse response = null;
    HttpRequest firstReq = httpRequest.getWithIO();
    try {
      response = firstReq.execute();
    } catch (HttpResponseException firstReqException) {
      Optional<HttpResponse> maybeTokenRefreshedRetry = Optional.empty();
      try {
        maybeTokenRefreshedRetry =
            maybeRetryWithFreshToken(httpRequest, firstReqException);
      } catch (HttpResponseException tokenRefreshedRetryException) {
        rethrowForDtpStandards(
            tokenRefreshedRetryException.getStatusCode(),
            Optional.of(tokenRefreshedRetryException),
            Optional.empty() /*maybeResponse*/);
      }

      if (maybeTokenRefreshedRetry.isPresent()) {
        response = maybeTokenRefreshedRetry.get();
      } else {
        rethrowForDtpStandards(
            firstReqException.getStatusCode(),
            Optional.of(firstReqException),
            Optional.empty() /*maybeResponse*/);
      }
    }
    Preconditions.checkNotNull(
        response,
        "bug? response should be set, else DTP error already thrown, but neither happened?");

    if (response.getStatusCode() != 200) {
      rethrowForDtpStandards(
        response.getStatusCode(),
        Optional.empty() /*maybeException*/,
        Optional.ofNullable(response));
    }

    return response;
  }

  private Optional<HttpResponse> maybeRetryWithFreshToken(
      SupplierWithIO<HttpRequest> httpRequest, HttpResponseException e)
      throws IOException, InvalidTokenException {
    // if the response is "unauthorized", refresh the token and try the request again
    final int statusCode = e.getStatusCode();

    if (statusCode != 401) {
      return Optional.empty();
    }

    monitor.info(() -> String.format("GooglePhotosInterface: Attempting to refresh authorization token due to HTTP response code=%s, %s\n", statusCode, e));
    // if the credential refresh failed, let the error bubble up via the IOException that gets
    // thrown
    credential = credentialFactory.refreshCredential(credential);
    monitor.info(() -> "GooglePhotosInterface: Refreshed authorization token successfully");

    // if the second attempt throws an error, then something else is wrong, and we bubble up the
    // response errors
    return Optional.of(httpRequest.getWithIO().execute());
  }

  /**
   * Tries to throw a DTP-standard exception for a request that's failed, given whatever info we
   * have about the failure (HTTP response code, if nothing else) .
   */
  // TODO: jzacsh rework this class to interact with HTTP in an easily
  // loggable/testable/re-usable way, like we did with MicrosoftApiResponse.
  private void rethrowForDtpStandards(
    int statusCode,
    Optional<HttpResponseException> maybeException,
    Optional<HttpResponse> maybeResponse)
  throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException { 
    final String emptyServerMessage = "[no server message: have neither response nor exception]";
    final Optional<String> serverMessage;
    if (maybeException.isPresent()) {
      serverMessage = Optional.of(maybeException.get().getContent());
    } else if (maybeResponse.isPresent()) {
      serverMessage = Optional.of(maybeResponse.get().getStatusMessage());
    } else {
      serverMessage = Optional.empty();
    }

    if (statusCode == 403 &&
        (serverMessage.orElse("").contains(GOOGPHOTOS_ALBUMS_PERMISSION_ERROR) ||
        serverMessage.orElse("").contains(GOOGPHOTOS_PHOTO_PERMISSION_ERROR))) {
      throw new PermissionDeniedException("User permission to google photos was denied", maybeException.orElse(null));
    }

   // Upload-related exceptions; currently this is only used for payload hash verifications.
    if (statusCode == 400 &&
      (serverMessage.orElse("").contains(GOOG_ERROR_HASH_MISMATCH_LEGACY) ||
      serverMessage.orElse("").contains(GOOG_ERROR_HASH_MISMATCH_UNIFIED))) {
    Throwable throwableForBadResponse =
        new IOException(String.format(
          "non-error HTTP response statusCode=%s: %s",
          statusCode,
          serverMessage.orElse(emptyServerMessage)));
    Throwable cause = maybeException.map(e -> (Throwable) e /*downcast*/).orElse(throwableForBadResponse);
    throw new UploadErrorException(ERROR_HASH_MISMATCH, cause);
    }

    // something else is wrong, bubble up the error
    throw new IOException(
        String.format("Bad HTTP response: status code=%s, Error='%s' Content: %s",
            statusCode,
            maybeException.map(e -> e.getStatusMessage()).orElse("[no HTTP error]"),
            serverMessage.orElse(emptyServerMessage)));
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
