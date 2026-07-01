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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AmazonPhotosClientTest {

  private MockWebServer server;
  private AmazonPhotosClient client;
  private File tempFile;

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

    tempFile = File.createTempFile("test", ".jpg");
    tempFile.deleteOnExit();
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
    assertEquals(ACCESS_TOKEN, request.getHeader("x-amz-access-token"));
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
  void uploadPhoto_sendsContentAndHeaders() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"photo1\",\"name\":\"test.jpg\",\"kind\":\"FILE\"}"));

    AmazonPhotosNode node = client.uploadPhoto(
        "test.jpg", tempFile, "md5hex", tempFile.length(),
        "2024-01-15T10:00:00Z", false, null);

    assertEquals("photo1", node.getId());

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("name=test.jpg"));
    assertTrue(request.getPath().contains("kind=FILE"));
    assertTrue(request.getPath().contains("conflictResolution=RENAME"));
    assertFalse(request.getPath().contains("visualCollectionParentNodeId"));
    assertEquals("md5hex", request.getHeader("x-amzn-file-md5"));
  }

  @Test
  void uploadPhoto_includesParentNodeId() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"p1\",\"name\":\"pic.png\",\"kind\":\"FILE\"}"));

    client.uploadPhoto("pic.png", tempFile, "md5", 1, null, false, "album1");

    RecordedRequest request = server.takeRequest();
    assertTrue(request.getPath().contains("visualCollectionParentNodeId=album1"));
  }

  @Test
  void uploadPhoto_includesFavoriteSetting() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"f1\",\"name\":\"fav.jpg\",\"kind\":\"FILE\"}"));

    client.uploadPhoto("fav.jpg", tempFile, "md5", 1, null, true, null);

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
    assertEquals("new-token", retryRequest.getHeader("x-amz-access-token"));
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
  void uploadPhoto_duplicateReturnsIOExceptionWith409() throws Exception {
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(409).setBody(
        "{\"errorCode\":\"DuplicatesConflictError\",\"errorDetails\":{\"conflictNodeIds\":[\"existingId\"]}}"));

    IOException ex = assertThrows(IOException.class,
        () -> client.uploadPhoto("dup.jpg", tempFile, "md5", 1, null, false, "album1"));

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
}
