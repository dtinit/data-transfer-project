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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AmazonPhotosClientTest {

  private MockWebServer server;
  private AmazonPhotosClient client;
  private File tempFile;

  @TempDir
  Path tempDir;

  private static final String ACCESS_TOKEN = "test-access-token";
  private static final String REFRESH_TOKEN = "test-refresh-token";
  private static final String CLIENT_ID = "test-client-id";
  private static final String CLIENT_SECRET = "test-client-secret";

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();

    // Interceptor routes all requests to MockWebServer regardless of original host
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          okhttp3.HttpUrl originalUrl = chain.request().url();
          okhttp3.HttpUrl newUrl = originalUrl.newBuilder()
              .scheme("http")
              .host(server.getHostName())
              .port(server.getPort())
              .build();
          return chain.proceed(chain.request().newBuilder().url(newUrl).build());
        })
        .build();

    client = new AmazonPhotosClient(httpClient, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);

    // resolveEndpoints call
    enqueueEndpointResponse();
    client.resolveEndpoints();

    tempFile = tempDir.resolve("test.jpg").toFile();
    Files.write(tempFile.toPath(), new byte[]{1, 2, 3});
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void resolveEndpoints_sendsAuthHeader() throws Exception {
    RecordedRequest request = server.takeRequest();
    assertEquals("GET", request.getMethod());
    assertEquals("Bearer " + ACCESS_TOKEN, request.getHeader("Authorization"));
    assertTrue(request.getPath().contains("/drive/v1/account/endpoint"));
  }

  @Test
  void createAlbum_sendsCorrectRequest() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"node123\",\"name\":\"My Album\",\"kind\":\"VISUAL_COLLECTION\"}"));

    AmazonPhotosNode node = client.createAlbum("My Album");

    assertEquals("node123", node.getId());
    assertEquals("My Album", node.getName());

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("resourceVersion=V2"));
    assertFalse(request.getPath().contains("//"));
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("\"name\":\"My Album\""));
    assertTrue(body.contains("\"kind\":\"VISUAL_COLLECTION\""));
    assertFalse(body.contains("parents"));
  }

  @Test
  void createAlbum_withDifferentName() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"album2\",\"name\":\"Vacation\",\"kind\":\"VISUAL_COLLECTION\"}"));

    AmazonPhotosNode node = client.createAlbum("Vacation");

    assertEquals("album2", node.getId());

    RecordedRequest request = server.takeRequest();
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("\"kind\":\"VISUAL_COLLECTION\""));
    assertTrue(body.contains("\"name\":\"Vacation\""));
    assertFalse(body.contains("parents"));
  }

  @Test
  void uploadContent_sendsContentAndHeaders() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"photo1\",\"name\":\"test.jpg\",\"kind\":\"FILE\"}"));

    AmazonPhotosNode node = client.uploadContent(
        "test.jpg", tempFile, "md5hex", tempFile.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("photo1", node.getId());

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("name=test.jpg"));
    assertTrue(request.getPath().contains("kind=FILE"));
    assertFalse(request.getPath().contains("visualCollectionParentNodeId"));
    assertEquals("md5hex", request.getHeader("x-amzn-file-md5"));
    // conflictResolution is in the metadata JSON body, not query params
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("RENAME"));
  }

  @Test
  void uploadContent_includesParentNodeId() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"p1\",\"name\":\"pic.png\",\"kind\":\"FILE\"}"));

    client.uploadContent("pic.png", tempFile, "md5", 1, null, false, "album1");

    RecordedRequest request = server.takeRequest();
    assertTrue(request.getPath().contains("visualCollectionParentNodeId=album1"));
  }

  @Test
  void uploadContent_includesFavoriteSetting() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"f1\",\"name\":\"fav.jpg\",\"kind\":\"FILE\"}"));

    client.uploadContent("fav.jpg", tempFile, "md5", 1, null, true, null);

    RecordedRequest request = server.takeRequest();
    assertTrue(request.getPath().contains("isFavorite=true"));
  }

  @Test
  void tokenRefresh_retriesOnUnauthorized() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(new MockResponse()
        .setBody("{\"access_token\":\"new-token\",\"token_type\":\"bearer\"}"));
    server.enqueue(new MockResponse().setBody("{\"id\":\"x\",\"name\":\"test\"}"));

    client.createAlbum("test");

    server.takeRequest(); // 401
    server.takeRequest(); // token refresh
    RecordedRequest retryRequest = server.takeRequest();
    assertEquals("Bearer new-token", retryRequest.getHeader("Authorization"));
  }

  @Test
  void serverError_throwsIOException() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"ISE\"}"));

    IOException ex = assertThrows(IOException.class,
        () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("500"));
  }

  @Test
  void uploadContent_duplicateReturnsIOExceptionWith409() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(409).setBody(
        "{\"errorCode\":\"DuplicatesConflictError\",\"errorDetails\":{\"conflictNodeIds\":[\"existingId\"]}}"));

    IOException ex = assertThrows(IOException.class,
        () -> client.uploadContent("dup.jpg", tempFile, "md5", 1, null, false, "album1"));

    assertTrue(ex.getMessage().contains("409"));
    assertTrue(ex.getMessage().contains("DuplicatesConflictError"));
  }

  @Test
  void resolveEndpoints_throwsOnFailure() throws Exception {
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          okhttp3.HttpUrl newUrl = chain.request().url().newBuilder()
              .scheme("http").host(server.getHostName()).port(server.getPort()).build();
          return chain.proceed(chain.request().newBuilder().url(newUrl).build());
        })
        .build();

    AmazonPhotosClient freshClient = new AmazonPhotosClient(
        httpClient, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);

    server.takeRequest(); // consume the setUp endpoint request

    server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"Not Registered\"}"));
    server.enqueue(new MockResponse().setBody("{\"access_token\":\"t\"}"));
    server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"Not Registered\"}"));

    IOException ex = assertThrows(IOException.class, freshClient::resolveEndpoints);
    assertTrue(ex.getMessage().contains("403"));
  }

  @Test
  void tokenRefreshFailure_throwsIOException() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    IOException ex = assertThrows(IOException.class,
        () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("Token refresh failed"));
  }

  @Test
  void clientError403_throwsIOException() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"Forbidden\"}"));

    IOException ex = assertThrows(IOException.class,
        () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("403"));
  }

  private void enqueueEndpointResponse() {
    server.enqueue(new MockResponse().setBody(
        "{\"metadataUrl\":\"https://meta.example.com/v1\","
            + "\"contentUrl\":\"https://content.example.com\","
            + "\"uploadServiceUrl\":\"https://upload.example.com/\"}"));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Multipart upload tests
  // ─────────────────────────────────────────────────────────────────────────

  private File createMultipartTestFile(int size) throws Exception {
    File file = tempDir.resolve("multipart-test-" + size + ".bin").toFile();
    byte[] data = new byte[size];
    for (int i = 0; i < size; i++) { data[i] = (byte) (i % 256); }
    Files.write(file.toPath(), data);
    return file;
  }

  private void enqueueInitiateResponse(String nodeId, String uploadId, long partSize, int totalParts) {
    server.enqueue(new MockResponse().setBody(String.format(
        "{\"nodeId\":\"%s\",\"uploadId\":\"%s\",\"partSize\":%d,\"totalNumberOfParts\":%d}",
        nodeId, uploadId, partSize, totalParts)));
  }

  private void enqueuePartResponse() {
    server.enqueue(new MockResponse().setBody("{\"status\":\"UPLOAD_IN_PROGRESS\"}"));
  }

  private void enqueueCompleteResponse() {
    server.enqueue(new MockResponse().setBody("{\"status\":\"UPLOAD_COMPLETING\"}"));
  }

  private void enqueuePollResponse(String status) {
    server.enqueue(new MockResponse().setBody(String.format("{\"status\":\"%s\"}", status)));
  }

  @Test
  void uploadContent_multipart_happyPath() throws Exception {
    server.takeRequest(); // endpoint resolution
    client.multipartThreshold = 10; // trigger multipart for files > 10 bytes

    // File: 15 bytes, server says partSize=5, totalParts=3
    File file = createMultipartTestFile(15);

    enqueueInitiateResponse("node-mp1", "upload-123", 5, 3);
    enqueuePartResponse(); // part 1
    enqueuePartResponse(); // part 2
    enqueuePartResponse(); // part 3
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    AmazonPhotosNode node = client.uploadContent(
        "video.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", false, "album-1");

    assertEquals("node-mp1", node.getId());

    // Verify request sequence: initiate, 3 parts, complete, poll
    RecordedRequest initReq = server.takeRequest();
    assertEquals("POST", initReq.getMethod());
    assertTrue(initReq.getPath().contains("multipart-upload"));
    assertTrue(initReq.getPath().contains("name=video.mp4"));
    assertTrue(initReq.getPath().contains("visualCollectionParentNodeId=album-1"));

    for (int i = 1; i <= 3; i++) {
      RecordedRequest partReq = server.takeRequest();
      assertEquals("PUT", partReq.getMethod());
      assertTrue(partReq.getPath().contains("/parts/" + i));
      assertTrue(partReq.getPath().contains("uploadId=upload-123"));
    }

    RecordedRequest completeReq = server.takeRequest();
    assertEquals("POST", completeReq.getMethod());
    assertTrue(completeReq.getPath().contains("/complete"));

    RecordedRequest pollReq = server.takeRequest();
    assertEquals("GET", pollReq.getMethod());
    assertTrue(pollReq.getPath().contains("node-mp1"));
    assertTrue(pollReq.getPath().contains("uploadId=upload-123"));
  }

  @Test
  void uploadContent_multipart_lastPartialPart() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 10;

    // File: 12 bytes, partSize=5, totalParts=3 → parts are 5, 5, 2
    File file = createMultipartTestFile(12);

    enqueueInitiateResponse("node-mp2", "upload-456", 5, 3);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    AmazonPhotosNode node = client.uploadContent(
        "clip.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("node-mp2", node.getId());

    server.takeRequest(); // initiate
    RecordedRequest part1 = server.takeRequest();
    RecordedRequest part2 = server.takeRequest();
    RecordedRequest part3 = server.takeRequest();

    // Last part should be 2 bytes
    assertTrue(part1.getPath().contains("partSize=5"));
    assertTrue(part2.getPath().contains("partSize=5"));
    assertTrue(part3.getPath().contains("partSize=2"));
  }

  @Test
  void uploadContent_multipart_uploadFailed_throws() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp3", "upload-789", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_FAILED");

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("fail.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("Multipart upload failed"));
  }

  @Test
  void uploadContent_multipart_completingThenSucceeds() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp4", "upload-abc", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    // Server returns COMPLETING twice, then succeeds
    enqueuePollResponse("UPLOAD_COMPLETING");
    enqueuePollResponse("UPLOAD_COMPLETING");
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    AmazonPhotosNode node = client.uploadContent(
        "slow.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("node-mp4", node.getId());
  }

  @Test
  void uploadContent_multipart_inProgress_throwsImmediately() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp5", "upload-def", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_IN_PROGRESS");

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("inprog.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("UPLOAD_IN_PROGRESS"));
    assertTrue(ex.getMessage().contains("Multipart upload failed"));
  }

  @Test
  void uploadContent_multipart_aborted_throwsImmediately() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp6", "upload-ghi", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_ABORTED");

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("abort.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("UPLOAD_ABORTED"));
  }

  @Test
  void uploadContent_multipart_expired_throwsImmediately() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp7", "upload-jkl", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_EXPIRED");

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("expire.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("UPLOAD_EXPIRED"));
  }

  @Test
  void getMultipartUploadProgress_resumeSupport() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse().setBody(
        "{\"status\":\"UPLOAD_COMPLETING\",\"partSize\":1024,\"totalNumberOfParts\":5}"));

    com.fasterxml.jackson.databind.JsonNode progress =
        client.getMultipartUploadProgress("resume-node", "resume-upload");

    assertEquals("UPLOAD_COMPLETING", progress.get("status").asText());
    assertEquals(5, progress.get("totalNumberOfParts").asInt());
  }

  @Test
  void uploadContent_multipart_transientProgressError_retriesUntilSuccess() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;
    client.initialPollDelayMs = 0;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp8", "upload-mno", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    // First progress check errors (500); the upload must not fail -- it should keep polling.
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"transient\"}"));
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    AmazonPhotosNode node = client.uploadContent(
        "retry.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("node-mp8", node.getId());
  }

  @Test
  void uploadContent_multipart_progressCheck4xx_failsFast() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp9", "upload-pqr", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    // Progress check returns a definitive 4xx -> must fail fast (only one poll enqueued);
    // if it wrongly retried, the test would block waiting for more responses.
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"errorCode\":\"MultipartUploadNotFound\"}"));

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("nf.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("404"));
  }

  @Test
  void uploadContent_multipart_missingStatusField_retriesUntilSuccess() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;
    client.initialPollDelayMs = 0;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-mp10", "upload-stu", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    // 2xx but no "status" field -> malformed; must be retried (not NPE out of the poll loop).
    server.enqueue(new MockResponse().setBody("{}"));
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    AmazonPhotosNode node = client.uploadContent(
        "ms.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("node-mp10", node.getId());
  }

  @Test
  void error_logsErrorCodeAndMessageTogether() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse().setResponseCode(403).setBody(
        "{\"errorCode\":\"ForbiddenAccess\",\"message\":\"App not authorized\"}"));

    IOException ex = assertThrows(IOException.class, () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("403"));
    assertTrue(ex.getMessage().contains("ForbiddenAccess"));
    assertTrue(ex.getMessage().contains("App not authorized"));
  }

  @Test
  void createDefaultHttpClient_hasConfiguredTimeouts() {
    OkHttpClient c = AmazonPhotosClient.createDefaultHttpClient();
    assertEquals(30000, c.connectTimeoutMillis());
    assertEquals(60000, c.readTimeoutMillis());
    assertEquals(30000, c.writeTimeoutMillis());
  }

  @Test
  void resolveEndpoints_missingUploadServiceUrl_throwsIncomplete() throws Exception {
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          okhttp3.HttpUrl newUrl = chain.request().url().newBuilder()
              .scheme("http").host(server.getHostName()).port(server.getPort()).build();
          return chain.proceed(chain.request().newBuilder().url(newUrl).build());
        })
        .build();
    AmazonPhotosClient freshClient = new AmazonPhotosClient(
        httpClient, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);
    server.takeRequest(); // consume the setUp endpoint request

    // metadataUrl present but uploadServiceUrl absent -> resolution is incomplete.
    server.enqueue(new MockResponse().setBody(
        "{\"metadataUrl\":\"https://meta.example.com/v1\",\"contentUrl\":\"https://c.example.com\"}"));

    IOException ex = assertThrows(IOException.class, freshClient::resolveEndpoints);
    assertTrue(ex.getMessage().contains("incomplete"));
  }

  @Test
  void error_nonJsonBody_reportsStatusOnly() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse().setResponseCode(500).setBody("not-json"));

    IOException ex = assertThrows(IOException.class, () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("500"));
    // Body is not JSON, so errorCode/message cannot be extracted and are omitted.
    assertFalse(ex.getMessage().contains("errorCode"));
  }

  @Test
  void uploadContent_multipart_completingBudgetExhausted_throws() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;
    client.pollMaxRetries = 2;
    client.initialPollDelayMs = 0;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-be1", "upload-be1", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_COMPLETING");
    enqueuePollResponse("UPLOAD_COMPLETING");

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("be.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("did not complete within 2 retries"));
  }

  @Test
  void uploadContent_multipart_transientProgressBudgetExhausted_throws() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;
    client.pollMaxRetries = 2;
    client.initialPollDelayMs = 0;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-be2", "upload-be2", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    // Progress check keeps returning 500 (transient) -> retried until budget is exhausted.
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"ise\"}"));
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"ise\"}"));

    IOException ex = assertThrows(IOException.class, () ->
        client.uploadContent("be2.mp4", file, "md5hex", file.length(),
            "2024-01-15T10:00:00Z", false, null));

    assertTrue(ex.getMessage().contains("progress check did not succeed within 2 retries"));
  }

  @Test
  void uploadContent_multipart_isFavorite_addsFavoriteParam() throws Exception {
    server.takeRequest(); // endpoint
    client.multipartThreshold = 5;
    client.initialPollDelayMs = 0;

    File file = createMultipartTestFile(10);

    enqueueInitiateResponse("node-fav", "upload-fav", 5, 2);
    enqueuePartResponse();
    enqueuePartResponse();
    enqueueCompleteResponse();
    enqueuePollResponse("UPLOAD_SUCCEEDED");

    client.uploadContent("fav.mp4", file, "md5hex", file.length(),
        "2024-01-15T10:00:00Z", true, null);

    RecordedRequest init = server.takeRequest();
    assertTrue(init.getPath().contains("isFavorite=true"));
  }

  @Test
  void error_emptyBody_reportsStatusOnly() throws Exception {
    server.takeRequest(); // endpoint

    server.enqueue(new MockResponse().setResponseCode(500)); // no body -> ""

    IOException ex = assertThrows(IOException.class, () -> client.createAlbum("test"));

    assertTrue(ex.getMessage().contains("500"));
    assertFalse(ex.getMessage().contains("errorCode"));
  }

  @Test
  void createAlbum_resolvesEndpointsLazilyWhenNotYetResolved() throws Exception {
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          okhttp3.HttpUrl newUrl = chain.request().url().newBuilder()
              .scheme("http").host(server.getHostName()).port(server.getPort()).build();
          return chain.proceed(chain.request().newBuilder().url(newUrl).build());
        })
        .build();
    // Fresh client: resolveEndpoints() has NOT been called explicitly.
    AmazonPhotosClient freshClient = new AmazonPhotosClient(
        httpClient, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);
    server.takeRequest(); // consume the setUp endpoint request

    enqueueEndpointResponse(); // served by the lazy ensureEndpointsResolved()
    server.enqueue(new MockResponse().setBody("{\"id\":\"n1\",\"name\":\"Lazy\"}"));

    AmazonPhotosNode node = freshClient.createAlbum("Lazy");

    assertEquals("n1", node.getId());
    RecordedRequest endpointReq = server.takeRequest();
    assertTrue(endpointReq.getPath().contains("/drive/v1/account/endpoint"));
  }

}
