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
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.datatransferproject.transfer.amazon.photos.model.CreateNodeRequest;
import org.datatransferproject.transfer.amazon.photos.model.EndpointResponse;
import org.datatransferproject.transfer.amazon.photos.model.MultipartUploadInitResponse;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  // HTTP client timeout configuration
  private static final int CONNECT_TIMEOUT_SECONDS = 30;
  private static final int READ_TIMEOUT_SECONDS = 60;
  private static final int WRITE_TIMEOUT_SECONDS = 30;

  private static final String RESOURCE_VERSION = "V2";
  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String MD5_HEADER = "x-amzn-file-md5";
  private static final String SLASH = "/";
  private static final String NODES_PATH = "nodes";
  private static final String UPLOAD_PATH = "v2/upload/multiform-upload";
  private static final String MULTIPART_UPLOAD_PATH = "v2/upload/multipart-upload";
  long multipartThreshold = 4_500_000_000L; // 4.5GB, package-private for testing
  private static final String PARAM_RESOURCE_VERSION = "resourceVersion";
  private static final String PARAM_CONFLICT_RESOLUTION = "conflictResolution";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_KIND = "kind";
  private static final String PARAM_FILE_SIZE = "fileSize";
  private static final String PARAM_PARENT_NODE_ID = "visualCollectionParentNodeId";
  private static final String PARAM_FALLBACK_CONTENT_DATE = "fallbackContentDate";
  private static final String PARAM_FAVORITE = "isFavorite";
  private static final String KIND_FILE = "FILE";
  private static final String KIND_ALBUM = "VISUAL_COLLECTION";
  private static final String CONFLICT_RESOLUTION_RENAME = "RENAME";
  private static final String PARAM_UPLOAD_ID = "uploadId";
  private static final String PARAM_PART_SIZE = "partSize";
  private static final String PART_MD5_HEADER = "x-amzn-part-md5";
  private static final String FORM_PART_METADATA = "metadata";
  private static final String FORM_PART_FILE = "file";
  private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
  private static final MediaType OCTET_STREAM_TYPE = MediaType.parse("application/octet-stream");
  // Empty request body for payload-less POSTs (initiate / complete multipart). OkHttp's POST
  // requires a non-null RequestBody, so a null/absent body is not an option.
  private static final RequestBody EMPTY_BODY = RequestBody.create(null, new byte[0]);

  private static final String UPLOAD_SUCCEEDED = "UPLOAD_SUCCEEDED";
  private static final String UPLOAD_COMPLETING = "UPLOAD_COMPLETING";

  // Polling config for waiting on server-side multipart completion.
  // After complete-upload is called the server stitches parts asynchronously;
  // larger files (tens of GBs) take significantly longer to process.
  // Backoff is exponential-with-jitter (1s, 2s, 4s, 8s, 16s, 32s, then capped at 60s),
  // so it reaches the cap by ~attempt 7. Budget: 85 retries ≈ 80 min total.
  static final int MAX_POLL_RETRIES = 85;
  static final double MAX_BACKOFF_SECONDS = 60.0;
  static final long INITIAL_DELAY_MS = 1500;

  // Package-private and mutable for testing (mirrors multipartThreshold): lets tests exercise the
  // retry-budget-exhausted paths and skip the initial delay without minutes-long real waits.
  int pollMaxRetries = MAX_POLL_RETRIES;
  long initialPollDelayMs = INITIAL_DELAY_MS;

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;
  private final Monitor monitor;

  private volatile String accessToken;
  private volatile String metadataUrl;
  private volatile String uploadServiceUrl;

  @FunctionalInterface
  interface AuthenticatedCall<T> {
    T execute(String token) throws IOException;
  }

  /**
   * Creates an OkHttpClient configured with appropriate timeouts for upload operations.
   */
  public static OkHttpClient createDefaultHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build();
  }

  public AmazonPhotosClient(OkHttpClient httpClient, String accessToken, String refreshToken,
                            String clientId, String clientSecret) {
    this(httpClient, accessToken, refreshToken, clientId, clientSecret, new Monitor() {});
  }

  public AmazonPhotosClient(OkHttpClient httpClient, String accessToken, String refreshToken,
                            String clientId, String clientSecret, Monitor monitor) {
    this.httpClient = httpClient;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.monitor = monitor;
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
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .get()
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        EndpointResponse endpoints = objectMapper.readValue(
            response.body().string(), EndpointResponse.class);
        if (endpoints.getMetadataUrl() == null || endpoints.getUploadServiceUrl() == null) {
          throw new IOException("Endpoint resolution returned incomplete response");
        }
        metadataUrl = normalizeUrl(endpoints.getMetadataUrl());
        // We require the upload service URL (not the content URL) from the response, because
        // multipart upload is only supported on the upload service URL. No fallback.
        uploadServiceUrl = normalizeUrl(endpoints.getUploadServiceUrl());
        // The token-refresh wrapper expects the lambda to return a value; this call resolves
        // endpoints as a side effect and has no result, so we return null.
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
      CreateNodeRequest nodeRequest = new CreateNodeRequest(name, KIND_ALBUM);

      HttpUrl url = HttpUrl.parse(metadataUrl + NODES_PATH).newBuilder()
          .addQueryParameter(PARAM_RESOURCE_VERSION, RESOURCE_VERSION)
          .build();

      Request request = new Request.Builder()
          .url(url)
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .post(RequestBody.create(
              JSON_MEDIA_TYPE, objectMapper.writeValueAsString(nodeRequest)))
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        return objectMapper.readValue(response.body().string(), AmazonPhotosNode.class);
      }
    });
  }

  /**
   * Uploads a file to Amazon Photos. Uses multipart upload for files > 4.5GB,
   * otherwise uses the simple multiform upload.
   *
   * @throws IOException if the upload fails, times out during server-side processing,
   *                     or the thread is interrupted while polling for completion
   */
  @Override
  public AmazonPhotosNode uploadContent(String fileName, File fileContent,
                                      String md5Hex, long fileSize, String fallbackContentDate,
                                      boolean isFavorite,
                                      String albumId) throws IOException {
    ensureEndpointsResolved();
    if (fileSize > multipartThreshold) {
      return uploadMultipart(fileName, fileContent, md5Hex, fileSize, fallbackContentDate, isFavorite, albumId);
    }
    return executeWithTokenRefresh(token -> {
      HttpUrl url = buildUploadUrl(fileName, fileSize, fallbackContentDate, isFavorite, albumId);

      String metadataJson = objectMapper.writeValueAsString(
          new CreateNodeRequest(fileName, KIND_FILE, CONFLICT_RESOLUTION_RENAME));

      RequestBody multipartBody = new okhttp3.MultipartBody.Builder()
          .setType(okhttp3.MultipartBody.FORM)
          .addFormDataPart(FORM_PART_METADATA, null,
              RequestBody.create(JSON_MEDIA_TYPE, metadataJson))
          .addFormDataPart(FORM_PART_FILE, fileName,
              RequestBody.create(OCTET_STREAM_TYPE, fileContent))
          .build();

      Request request = new Request.Builder()
          .url(url)
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .addHeader(MD5_HEADER, md5Hex)
          .post(multipartBody)
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        return objectMapper.readValue(response.body().string(), AmazonPhotosNode.class);
      }
    });
  }

  private AmazonPhotosNode uploadMultipart(String fileName, File fileContent,
                                           String md5Hex, long fileSize, String fallbackContentDate,
                                           boolean isFavorite, String albumId) throws IOException {
    MultipartUploadInitResponse init = initiateMultipartUpload(
        fileName, md5Hex, fileSize, fallbackContentDate, isFavorite, albumId);
    uploadAllParts(fileContent, fileSize, init);
    completeMultipartUpload(init);
    pollUntilUploadComplete(init.getNodeId(), init.getUploadId());

    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId(init.getNodeId());
    node.setName(fileName);
    return node;
  }

  private MultipartUploadInitResponse initiateMultipartUpload(
      String fileName, String md5Hex, long fileSize, String fallbackContentDate,
      boolean isFavorite, String albumId) throws IOException {
    return executeWithTokenRefresh(token -> {
      HttpUrl.Builder initUrl = HttpUrl.parse(uploadServiceUrl + MULTIPART_UPLOAD_PATH).newBuilder()
          .addQueryParameter(PARAM_NAME, fileName)
          .addQueryParameter(PARAM_KIND, KIND_FILE)
          .addQueryParameter(PARAM_FILE_SIZE, String.valueOf(fileSize))
          .addQueryParameter(PARAM_CONFLICT_RESOLUTION, CONFLICT_RESOLUTION_RENAME);
      if (albumId != null) {
        initUrl.addQueryParameter(PARAM_PARENT_NODE_ID, albumId);
      }
      if (fallbackContentDate != null) {
        initUrl.addQueryParameter(PARAM_FALLBACK_CONTENT_DATE, fallbackContentDate);
      }
      if (isFavorite) {
        initUrl.addQueryParameter(PARAM_FAVORITE, "true");
      }

      Request initRequest = new Request.Builder()
          .url(initUrl.build())
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .addHeader(MD5_HEADER, md5Hex)
          .post(EMPTY_BODY)
          .build();

      try (Response initResponse = httpClient.newCall(initRequest).execute()) {
        validateResponse(initResponse);
        return objectMapper.readValue(initResponse.body().string(), MultipartUploadInitResponse.class);
      }
    });
  }

  private void uploadAllParts(File fileContent, long fileSize,
                              MultipartUploadInitResponse init) throws IOException {
    String nodeId = init.getNodeId();
    String uploadId = init.getUploadId();
    long partSize = init.getPartSize();
    int totalParts = init.getTotalNumberOfParts();

    try (RandomAccessFile raf = new RandomAccessFile(fileContent, "r")) {
      for (int partNum = 1; partNum <= totalParts; partNum++) {
        long offset = (partNum - 1) * partSize;
        long length = Math.min(partSize, fileSize - offset);

        // Pass 1: compute MD5 with small buffer
        String partMd5 = Md5Utils.computeRegionMd5(raf, offset, length);

        // Pass 2: stream upload from file
        final int partNumber = partNum;
        final long partLength = length;
        executeWithTokenRefresh(token -> {
          HttpUrl partUrl = HttpUrl.parse(
              uploadServiceUrl + MULTIPART_UPLOAD_PATH + "/" + nodeId + "/parts/" + partNumber).newBuilder()
              .addQueryParameter(PARAM_UPLOAD_ID, uploadId)
              .addQueryParameter(PARAM_PART_SIZE, String.valueOf(partLength))
              .build();

          RequestBody body = createFileRegionBody(raf, offset, partLength);

          Request partRequest = new Request.Builder()
              .url(partUrl)
              .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
              .addHeader(PART_MD5_HEADER, partMd5)
              .put(body)
              .build();

          try (Response partResponse = httpClient.newCall(partRequest).execute()) {
            validateResponse(partResponse);
          }
          return null;
        });
      }
    }
  }

  private void completeMultipartUpload(MultipartUploadInitResponse init) throws IOException {
    executeWithTokenRefresh(token -> {
      HttpUrl completeUrl = HttpUrl.parse(
          uploadServiceUrl + MULTIPART_UPLOAD_PATH + "/" + init.getNodeId() + "/complete").newBuilder()
          .addQueryParameter(PARAM_UPLOAD_ID, init.getUploadId())
          .build();

      Request completeRequest = new Request.Builder()
          .url(completeUrl)
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .post(EMPTY_BODY)
          .build();

      try (Response completeResponse = httpClient.newCall(completeRequest).execute()) {
        validateResponse(completeResponse);
      }
      return null;
    });
  }

  private void pollUntilUploadComplete(String nodeId, String uploadId) throws IOException {
    long startMs = System.currentTimeMillis();

    // Initial delay before first poll to let server begin processing
    RetryUtils.sleep(initialPollDelayMs);

    for (int attempt = 1; attempt <= pollMaxRetries; attempt++) {
      final int currentAttempt = attempt;

      JsonNode progress;
      String status;
      try {
        progress = getMultipartUploadProgress(nodeId, uploadId);
        JsonNode statusNode = progress.path("status");
        if (!statusNode.isTextual()) {
          // A 2xx response with a missing/non-textual "status" is malformed; treat it as a
          // transient/retryable IOException rather than letting .asText() NPE out of the loop.
          throw new IOException(String.format(
              "Multipart progress response missing 'status' field for node: %s", nodeId));
        }
        status = statusNode.asText();
      } catch (IOException e) {
        // Fail fast on a definitive client error (4xx other than 429) -- the progress endpoint
        // will not recover (e.g. 404 upload-not-found, 403), so polling it through the whole
        // budget is pointless. Transient server errors (5xx / 429), network blips, and
        // malformed/unparseable bodies fall through and are retried until the budget is exhausted.
        if (e instanceof AmazonPhotosApiException) {
          int httpStatus = ((AmazonPhotosApiException) e).getHttpStatus();
          if (httpStatus >= 400 && httpStatus < 500 && httpStatus != 429) {
            throw e;
          }
        }
        monitor.info(() -> String.format(
            "[MultipartPoll] node=%s attempt=%d/%d progress check errored, will retry: %s",
            nodeId, currentAttempt, pollMaxRetries, e.getMessage()));
        if (attempt < pollMaxRetries) {
          RetryUtils.sleep(RetryUtils.computeBackoffMillis(attempt, MAX_BACKOFF_SECONDS));
          continue;
        }
        throw new IOException(String.format(
            "Multipart upload progress check did not succeed within %d retries for node: %s",
            pollMaxRetries, nodeId), e);
      }

      long elapsedMs = System.currentTimeMillis() - startMs;

      monitor.info(() -> String.format(
          "[MultipartPoll] node=%s attempt=%d/%d status=%s elapsed=%dms",
          nodeId, currentAttempt, pollMaxRetries, status, elapsedMs));

      if (UPLOAD_SUCCEEDED.equals(status)) {
        monitor.info(() -> String.format(
            "[MultipartPoll] node=%s completed: retries=%d durationMs=%d",
            nodeId, currentAttempt, elapsedMs));
        return;
      } else if (UPLOAD_COMPLETING.equals(status)) {
        if (attempt < pollMaxRetries) {
          long backoffMs = RetryUtils.computeBackoffMillis(attempt, MAX_BACKOFF_SECONDS);
          RetryUtils.sleep(backoffMs);
        }
      } else {
        throw new IOException(String.format(
            "Multipart upload failed for node: %s (status=%s, details=%s)",
            nodeId, status, progress));
      }
    }

    long totalMs = System.currentTimeMillis() - startMs;
    throw new IOException(String.format(
        "Multipart upload did not complete within %d retries (%dms) for node: %s",
        pollMaxRetries, totalMs, nodeId));
  }

  /**
   * Returns the current status and metadata of a multipart upload.
   */
  JsonNode getMultipartUploadProgress(String nodeId, String uploadId) throws IOException {
    return executeWithTokenRefresh(token -> {
      HttpUrl progressUrl = HttpUrl.parse(
          uploadServiceUrl + MULTIPART_UPLOAD_PATH + "/" + nodeId).newBuilder()
          .addQueryParameter(PARAM_UPLOAD_ID, uploadId)
          .build();

      Request request = new Request.Builder()
          .url(progressUrl)
          .addHeader(AUTH_HEADER, BEARER_PREFIX + token)
          .get()
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        validateResponse(response);
        return objectMapper.readTree(response.body().string());
      }
    });
  }

  /**
   * Creates a streaming RequestBody that reads a file region directly into the HTTP request,
   * avoiding loading the entire part into heap memory.
   */
  private static RequestBody createFileRegionBody(RandomAccessFile raf, long offset, long length) {
    return new RequestBody() {
      @Override public MediaType contentType() {
        return OCTET_STREAM_TYPE;
      }
      @Override public long contentLength() {
        return length;
      }
      @Override public void writeTo(okio.BufferedSink sink) throws IOException {
        raf.seek(offset);
        byte[] buf = new byte[8192];
        long remaining = length;
        while (remaining > 0) {
          int toRead = (int) Math.min(buf.length, remaining);
          raf.readFully(buf, 0, toRead);
          sink.write(buf, 0, toRead);
          remaining -= toRead;
        }
      }
    };
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
      String errorCode = extractJsonField(errorBody, "errorCode");
      String errorMessage = extractJsonField(errorBody, "message");

      String detail = Stream.of(
              errorCode != null ? "errorCode=" + errorCode : null,
              errorMessage != null ? "message=" + errorMessage : null)
          .filter(Objects::nonNull)
          .collect(Collectors.joining(", "));
      String message = "Amazon Photos API error: HTTP " + response.code()
          + (detail.isEmpty() ? "" : " (" + detail + ")");
      throw new AmazonPhotosApiException(response.code(), errorCode, message);
    }
  }

  /**
   * Extracts a top-level textual field from a JSON error body, or {@code null} if the body is
   * empty, not JSON, or the field is missing/non-textual. Only specific, reviewed fields
   * ({@code errorCode}, {@code message}) are surfaced -- the raw body is never embedded -- so
   * error logs stay diagnosable (code + message together) without leaking excess response detail.
   */
  private String extractJsonField(String errorBody, String field) {
    if (errorBody == null || errorBody.isEmpty()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(errorBody).get(field);
      return (node != null && node.isTextual()) ? node.asText() : null;
    } catch (IOException e) {
      return null;
    }
  }

  private HttpUrl buildUploadUrl(String fileName, long fileSize, String fallbackContentDate,
                                 boolean isFavorite, String albumId) {
    HttpUrl.Builder builder = HttpUrl.parse(uploadServiceUrl + UPLOAD_PATH).newBuilder()
        .addQueryParameter(PARAM_NAME, fileName)
        .addQueryParameter(PARAM_KIND, KIND_FILE)
        .addQueryParameter(PARAM_FILE_SIZE, String.valueOf(fileSize));

    if (albumId != null) {
      builder.addQueryParameter(PARAM_PARENT_NODE_ID, albumId);
    }
    if (fallbackContentDate != null) {
      builder.addQueryParameter(PARAM_FALLBACK_CONTENT_DATE, fallbackContentDate);
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
    if (metadataUrl == null || uploadServiceUrl == null) {
      synchronized (this) {
        if (metadataUrl == null || uploadServiceUrl == null) {
          resolveEndpoints();
        }
      }
    }
  }

  private static class TokenExpiredException extends IOException {
    TokenExpiredException() { super("Access token expired"); }
  }
}
