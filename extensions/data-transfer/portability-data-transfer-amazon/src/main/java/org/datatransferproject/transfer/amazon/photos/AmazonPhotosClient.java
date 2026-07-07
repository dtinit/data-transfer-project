/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.datatransferproject.transfer.amazon.photos.model.CreateNodeRequest;
import org.datatransferproject.transfer.amazon.photos.model.EndpointResponse;

import java.io.File;
import java.io.IOException;

/**
 * HTTP client for Amazon Photos APIs.
 *
 * <p>Handles endpoint resolution, token refresh on 401, simple and multipart uploads,
 * node creation, listing, downloading, and parent-child relationships.
 */
public class AmazonPhotosClient implements AmazonPhotosInterface {

  private static final String ENDPOINT_URL =
      "https://drive.amazonaws.com/drive/v1/account/endpoint";
  private static final String TOKEN_URL = "https://api.amazon.com/auth/o2/token";
  private static final String RESOURCE_VERSION = "V2";
  private static final String AUTH_HEADER = "x-amz-access-token";
  private static final String MD5_HEADER = "x-amzn-file-md5";
  private static final String SLASH = "/";
  private static final String NODES_PATH = "nodes";
  private static final String UPLOAD_PATH = "v2/upload/multiform-upload";
  private static final String PARAM_RESOURCE_VERSION = "resourceVersion";
  private static final String PARAM_CONFLICT_RESOLUTION = "conflictResolution";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_KIND = "kind";
  private static final String PARAM_FILE_SIZE = "fileSize";
  private static final String PARAM_PARENT_NODE_ID = "visualCollectionParentNodeId";
  private static final String PARAM_CONTENT_DATE = "fallbackContentDate";
  private static final String PARAM_FAVORITE = "isFavorite";
  private static final String KIND_FILE = "FILE";
  private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;

  private volatile String accessToken;
  private volatile String metadataUrl;
  private volatile String uploadServiceUrl;
  private volatile String contentUrl;

  @FunctionalInterface
  interface AuthenticatedCall<T> {
    T execute(String token) throws IOException;
  }

  public AmazonPhotosClient(OkHttpClient httpClient, String accessToken, String refreshToken,
                            String clientId, String clientSecret) {
    this.httpClient = httpClient;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Resolves regional API endpoints via the getEndpoint API.
   * Must be called before any other API method.
   */
  @Override
  public void resolveEndpoints() throws IOException {
    executeWithTokenRefresh(token -> {
      Request request = new Request.Builder()
          .url(ENDPOINT_URL)
          .addHeader(AUTH_HEADER, token)
          .get()
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        EndpointResponse endpoints = objectMapper.readValue(
            response.body().string(), EndpointResponse.class);
        if (endpoints.getMetadataUrl() == null || endpoints.getContentUrl() == null) {
          throw new IOException("Endpoint resolution returned incomplete response");
        }
        contentUrl = normalizeUrl(endpoints.getContentUrl());
        uploadServiceUrl = endpoints.getUploadServiceUrl() != null
            ? normalizeUrl(endpoints.getUploadServiceUrl())
            : contentUrl;
        metadataUrl = normalizeUrl(endpoints.getMetadataUrl());
        return null;
      }
    });
  }

  /**
   * Creates an album in Amazon Photos.
   */
  @Override
  public AmazonPhotosNode createAlbum(String name) throws IOException {
    ensureEndpointsResolved();
    return executeWithTokenRefresh(token -> {
      CreateNodeRequest nodeRequest = new CreateNodeRequest(name, "VISUAL_COLLECTION");

      HttpUrl url = HttpUrl.parse(metadataUrl + NODES_PATH).newBuilder()
          .addQueryParameter(PARAM_RESOURCE_VERSION, RESOURCE_VERSION)
          .build();

      Request request = new Request.Builder()
          .url(url)
          .addHeader(AUTH_HEADER, token)
          .post(RequestBody.create(
              objectMapper.writeValueAsString(nodeRequest), JSON_MEDIA_TYPE))
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        return objectMapper.readValue(response.body().string(), AmazonPhotosNode.class);
      }
    });
  }

  /**
   * Uploads a photo to Amazon Photos.
   *
   * TODO: Add multipart upload support for files > 5GB during video importer integration.
   *
   * @throws IOException on API errors including 409 duplicate conflict
   */
  @Override
  public AmazonPhotosNode uploadPhoto(String fileName, File fileContent,
                                      String md5Hex, long fileSize, String contentDate,
                                      boolean isFavorite,
                                      String albumId) throws IOException {
    ensureEndpointsResolved();
    return executeWithTokenRefresh(token -> {
      HttpUrl url = buildUploadUrl(fileName, fileSize, contentDate, isFavorite, albumId);

      String metadataJson = objectMapper.writeValueAsString(
          new CreateNodeRequest(fileName, KIND_FILE));

      RequestBody multipartBody = new okhttp3.MultipartBody.Builder()
          .setType(okhttp3.MultipartBody.FORM)
          .addFormDataPart("metadata", null,
              RequestBody.create(metadataJson, JSON_MEDIA_TYPE))
          .addFormDataPart("file", fileName,
              RequestBody.create(fileContent, MediaType.parse("application/octet-stream")))
          .build();

      Request request = new Request.Builder()
          .url(url)
          .addHeader(AUTH_HEADER, token)
          .addHeader(MD5_HEADER, md5Hex)
          .post(multipartBody)
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        return objectMapper.readValue(response.body().string(), AmazonPhotosNode.class);
      }
    });
  }

  private <T> T executeWithTokenRefresh(AuthenticatedCall<T> call) throws IOException {
    try {
      return call.execute(accessToken);
    } catch (TokenExpiredException e) {
      refreshAccessToken();
      return call.execute(accessToken);
    }
  }

  private synchronized void refreshAccessToken() throws IOException {
    RequestBody body = new FormBody.Builder()
        .add("grant_type", "refresh_token")
        .add("refresh_token", refreshToken)
        .add("client_id", clientId)
        .add("client_secret", clientSecret)
        .build();

    Request request = new Request.Builder().url(TOKEN_URL).post(body).build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Token refresh failed with status " + response.code());
      }
      JsonNode json = objectMapper.readTree(response.body().string());
      this.accessToken = json.get("access_token").asText();
    }
  }

  private void validateResponse(Response response) throws IOException {
    if (response.code() == 401) {
      throw new TokenExpiredException();
    }
    if (!response.isSuccessful()) {
      String errorBody = response.body() != null ? response.body().string() : "";
      throw new IOException("API error " + response.code() + ": " + errorBody);
    }
  }

  private HttpUrl buildUploadUrl(String fileName, long fileSize, String contentDate,
                                 boolean isFavorite, String albumId) {
    HttpUrl.Builder builder = HttpUrl.parse(uploadServiceUrl + UPLOAD_PATH).newBuilder()
        .addQueryParameter(PARAM_NAME, fileName)
        .addQueryParameter(PARAM_KIND, KIND_FILE)
        .addQueryParameter(PARAM_FILE_SIZE, String.valueOf(fileSize))
        .addQueryParameter(PARAM_CONFLICT_RESOLUTION, "RENAME");

    if (albumId != null) {
      builder.addQueryParameter(PARAM_PARENT_NODE_ID, albumId);
    }
    if (contentDate != null) {
      builder.addQueryParameter(PARAM_CONTENT_DATE, contentDate);
    }
    if (isFavorite) {
      builder.addQueryParameter(PARAM_FAVORITE, "true");
    }
    return builder.build();
  }

  private static String normalizeUrl(String url) {
    return url.endsWith(SLASH) ? url : url + SLASH;
  }

  private void ensureEndpointsResolved() throws IOException {
    if (metadataUrl == null) {
      synchronized (this) {
        if (metadataUrl == null) {
          resolveEndpoints();
        }
      }
    }
  }

  private static class TokenExpiredException extends IOException {
    TokenExpiredException() { super("Access token expired"); }
  }
}
