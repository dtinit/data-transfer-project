/*
 * Copyright 2020 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.koofr.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.client.utils.URIBuilder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** A minimal Koofr REST API client. */
public class KoofrClient {
  private final String baseUrl;
  private final OkHttpClient client;
  private final OkHttpClient fileUploadClient;
  private final ObjectMapper objectMapper;
  private final Monitor monitor;
  private final KoofrCredentialFactory credentialFactory;
  private Credential credential;
  private boolean rootEnsured;
  private boolean videosEnsured;

  private static final String API_PATH_PREFIX = "/api/v2";
  private static final String CONTENT_API_PATH_PREFIX = "/content/api/v2";
  private static final String ROOT_NAME = "Data transfer";
  private static final String VIDEOS_NAME = "Videos";

  public KoofrClient(
      String baseUrl,
      OkHttpClient client,
      OkHttpClient fileUploadClient,
      ObjectMapper objectMapper,
      Monitor monitor,
      KoofrCredentialFactory credentialFactory) {
    this.baseUrl = baseUrl;
    this.client = client;
    this.fileUploadClient = fileUploadClient;
    this.objectMapper = objectMapper;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.credential = null;
    this.rootEnsured = false;
    this.videosEnsured = false;
  }

  public boolean fileExists(String path) throws IOException, InvalidTokenException {
    String url;
    try {
      url =
          getUriBuilder()
              .setPath(API_PATH_PREFIX + "/mounts/primary/files/info")
              .setParameter("path", path)
              .build()
              .toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url);

    try (Response response = getResponse(requestBuilder)) {
      int code = response.code();
      if (code == 200) {
        return true;
      }
      if (code == 404) {
        return false;
      }
      throw new IOException(
          "Got error code: "
              + code
              + " message: "
              + response.message()
              + " body: "
              + response.body().string());
    }
  }

  public void ensureFolder(String parentPath, String name)
      throws IOException, InvalidTokenException {
    Map<String, Object> rawFolder = new LinkedHashMap<>();
    rawFolder.put("name", name);

    String url;
    try {
      url =
          getUriBuilder()
              .setPath(API_PATH_PREFIX + "/mounts/primary/files/folder")
              .setParameter("path", parentPath)
              .build()
              .toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url);
    requestBuilder.post(
        RequestBody.create(
            MediaType.parse("application/json"), objectMapper.writeValueAsString(rawFolder)));

    try (Response response = getResponse(requestBuilder)) {
      int code = response.code();
      // 409 response code means that the folder already exists
      if ((code < 200 || code > 299) && code != 409) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + response.body().string());
      }
    }
  }

  public void addDescription(String path, String description)
      throws IOException, InvalidTokenException {
    Map<String, String[]> tags = new LinkedHashMap<>();
    tags.put("description", new String[] {description});
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("tags", tags);

    String url;
    try {
      url =
          getUriBuilder()
              .setPath(API_PATH_PREFIX + "/mounts/primary/files/tags/add")
              .setParameter("path", path)
              .build()
              .toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url);
    requestBuilder.post(
        RequestBody.create(
            MediaType.parse("application/json"), objectMapper.writeValueAsString(body)));

    try (Response response = getResponse(requestBuilder)) {
      int code = response.code();
      if ((code < 200 || code > 299) && code != 409) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + response.body().string());
      }
    }
  }

  @SuppressWarnings("unchecked")
  public String uploadFile(
      String parentPath,
      String name,
      InputStream inputStream,
      String mediaType,
      Date modified,
      String description)
      throws IOException, InvalidTokenException, DestinationMemoryFullException {
    String url;
    try {
      URIBuilder builder =
          getUriBuilder()
              .setPath(CONTENT_API_PATH_PREFIX + "/mounts/primary/files/put")
              .setParameter("path", parentPath)
              .setParameter("filename", name)
              .setParameter("autorename", "true")
              .setParameter("info", "true");
      if (description != null && description.length() > 0) {
        builder.setParameter("tags", "description=" + description);
      }
      if (modified != null) {
        builder.setParameter("modified", Long.toString(modified.getTime()));
      }
      url = builder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url);

    RequestBody uploadBody = new InputStreamRequestBody(MediaType.parse(mediaType), inputStream);

    requestBuilder.post(uploadBody);

    // We need to reset the input stream because the request could already read some data
    try (Response response =
        getResponse(fileUploadClient, requestBuilder, () -> inputStream.reset())) {
      int code = response.code();
      ResponseBody body = response.body();
      if (code == 413) {
        throw new DestinationMemoryFullException(
            "Koofr quota exceeded", new Exception("Koofr file upload response code " + code));
      }
      if (code < 200 || code > 299) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + body.string());
      }

      Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
      String newName = (String) responseData.get("name");
      Preconditions.checkState(
          !Strings.isNullOrEmpty(newName), "Expected name value to be present in %s", responseData);
      return parentPath + "/" + newName;
    }
  }

  @SuppressWarnings("unchecked")
  public List<FilesListRecursiveItem> listRecursive(String path)
      throws IOException, InvalidTokenException {
    String url;
    try {
      URIBuilder builder =
          getUriBuilder()
              .setPath(CONTENT_API_PATH_PREFIX + "/mounts/primary/files/listrecursive")
              .setParameter("path", path);
      url = builder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url).get();

    try (Response response = getResponse(requestBuilder)) {
      int code = response.code();
      if (code == 404) {
        return ImmutableList.of();
      }
      ResponseBody body = response.body();
      if (code < 200 || code > 299) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + body.string());
      }

      try (final Reader bodyReader =
              new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8);
          final BufferedReader bufferedBodyReader = new BufferedReader(bodyReader); ) {
        String line;
        List<FilesListRecursiveItem> items = new ArrayList<FilesListRecursiveItem>();
        while ((line = bufferedBodyReader.readLine()) != null) {
          items.add(objectMapper.readValue(line, FilesListRecursiveItem.class));
        }
        return items;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public String fileLink(String path) throws IOException, InvalidTokenException {
    String url;
    try {
      url =
          getUriBuilder()
              .setPath(API_PATH_PREFIX + "/mounts/primary/files/download")
              .setParameter("path", path)
              .build()
              .toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }

    Request.Builder requestBuilder = getRequestBuilder(url);

    try (Response response = getResponse(requestBuilder)) {
      int code = response.code();
      ResponseBody body = response.body();
      if (code < 200 || code > 299) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + body.string());
      }

      Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);

      return (String) responseData.get("link");
    }
  }

  public String getRootPath() {
    return "/" + ROOT_NAME;
  }

  public String ensureRootFolder() throws IOException, InvalidTokenException {
    if (!rootEnsured) {
      ensureFolder("/", ROOT_NAME);
      rootEnsured = true;
    }

    return getRootPath();
  }

  public String ensureVideosFolder() throws IOException, InvalidTokenException {
    String rootFolder = ensureRootFolder();

    if (!videosEnsured) {
      ensureFolder(rootFolder, VIDEOS_NAME);
      videosEnsured = true;
    }

    return rootFolder + "/" + VIDEOS_NAME;
  }

  public Credential getOrCreateCredential(TokensAndUrlAuthData authData) {
    if (this.credential == null) {
      this.credential = this.credentialFactory.createCredential(authData);
    }
    return this.credential;
  }

  private Request.Builder getRequestBuilder(String url) {
    Request.Builder requestBuilder = new Request.Builder().url(url);
    requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    requestBuilder.header("X-Koofr-Version", "2.1");
    return requestBuilder;
  }

  private URIBuilder getUriBuilder() {
    try {
      return new URIBuilder(baseUrl);
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }
  }

  private Response getResponse(
      OkHttpClient httpClient, Request.Builder requestBuilder, OnRetry onRetry)
      throws IOException, InvalidTokenException {
    Response response = client.newCall(requestBuilder.build()).execute();

    if (response.code() == 401) {
      response.close();

      // If there was an unauthorized error, then try refreshing the creds
      credentialFactory.refreshCredential(credential);
      monitor.info(() -> "Refreshed authorization token successfuly");

      requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());

      if (onRetry != null) {
        onRetry.run();
      }

      response = httpClient.newCall(requestBuilder.build()).execute();
    }

    return response;
  }

  private Response getResponse(Request.Builder requestBuilder)
      throws IOException, InvalidTokenException {
    return getResponse(client, requestBuilder, null);
  }

  public static String trimDescription(String description) {
    if (description == null) {
      return description;
    }
    if (description.length() > 1000) {
      return description.substring(0, 1000);
    }
    return description;
  }

  @FunctionalInterface
  private interface OnRetry {
    void run() throws IOException, InvalidTokenException;
  }
}
