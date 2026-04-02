/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.models.C2Api;
import org.datatransferproject.datatransfer.synology.models.ServiceConfig;
import org.datatransferproject.datatransfer.synology.utils.ServiceConfigParser;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.NoNasInAccountException;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

/** Service for handling Synology DTP operations. */
public class SynologyDTPService {
  private final Monitor monitor;
  private final String exportingService;
  private final ObjectMapper objectMapper;
  private final OkHttpClient client;
  private final JobStore jobStore;
  private final SynologyOAuthTokenManager tokenManager;
  C2Api c2Api;
  ServiceConfig.Retry retryConfig;

  @FunctionalInterface
  protected interface RequestBodyGenerator {
    RequestBody get() throws CopyExceptionWithFailureReason, IOException;
  }

  /**
   * Constructs a new {@code SynologyDTPService} instance.
   *
   * @param monitor the monitor
   * @param exportingService the exporting service
   * @param jobStore the job store
   * @param tokenManager the token manager
   * @param client the HTTP client
   */
  public SynologyDTPService(
      Monitor monitor,
      TransferServiceConfig transferServiceConfig,
      String exportingService,
      JobStore jobStore,
      SynologyOAuthTokenManager tokenManager,
      OkHttpClient client) {
    ServiceConfig serviceConfig = ServiceConfigParser.parse(transferServiceConfig);
    this.c2Api = serviceConfig.getServiceAs("c2", C2Api.class);
    this.retryConfig = serviceConfig.getRetry();

    this.monitor = monitor;
    this.exportingService = exportingService;
    this.objectMapper = new ObjectMapper();
    this.jobStore = jobStore;
    this.tokenManager = tokenManager;
    this.client = configureClient(client);
  }

  @VisibleForTesting
  protected OkHttpClient configureClient(OkHttpClient client) {
    return client
        .newBuilder()
        .protocols(Arrays.asList(okhttp3.Protocol.HTTP_1_1))
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build();
  }

  /**
   * Creates album.
   *
   * @param album the album
   * @param jobId the job ID
   * @return a map of shape {"data": {"album_id": <album_id>}}
   */
  public Map<String, Object> createAlbum(MediaAlbum album, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    FormBody.Builder builder = new FormBody.Builder().add("title", album.getName());
    builder.add("album_id", album.getId());
    builder.add("job_id", jobId.toString());
    builder.add("service", exportingService);
    monitor.info(() -> "[SynologyImporter] Creating album", album.getName(), jobId);

    RequestBody requestBody = builder.build();
    return (Map<String, Object>)
        sendPostRequest(c2Api.getCreateAlbum(), () -> requestBody, jobId).get("data");
  }

  /**
   * get InputStreamWrapper for media file, it can be from temp store or from fetchable url
   *
   * @param jobId the job ID
   * @param fetchableUrl the url to fetch media file, can be null if the file is in temp store
   * @return an InputStreamWrapper instance
   * @throws CopyExceptionWithFailureReason
   */
  @VisibleForTesting
  protected InputStreamWrapper getMediaInputStreamWrapper(
      UUID jobId, String fetchableUrl, boolean isInTempStore) throws IOException {
    if (isInTempStore) {
      return jobStore.getStream(jobId, fetchableUrl);
    } else if (fetchableUrl != null) {
      URL url = new URL(fetchableUrl);
      URLConnection connection = url.openConnection();
      return new InputStreamWrapper(connection.getInputStream(), connection.getContentLengthLong());
    }

    throw new IllegalArgumentException("fetchableUrl is null and isInTempStore is false");
  }

  /**
   * Creates photo.
   *
   * @param photo the photo
   * @param jobId the job ID
   * @return a map of shape {"data": {"item_id": <item_id>}}
   */
  public Map<String, Object> createPhoto(PhotoModel photo, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    monitor.info(
        () ->
            String.format(
                "[SynologyImporter] starts creating photo, dataId: [%s], name: [%s].",
                photo.getDataId(), photo.getTitle()),
        jobId);

    RequestBodyGenerator bodyGenerator =
        () -> {
          // Due to InputStream may not repeatable, we need to open it inside the generator function
          // to make sure it can be read when retrying.
          InputStreamWrapper inputStreamWrapper =
              getMediaInputStreamWrapper(jobId, photo.getFetchableUrl(), photo.isInTempStore());

          RequestBody fileBody =
              new RequestBody() {
                private boolean isConsumed = false;

                @Override
                public MediaType contentType() {
                  return MediaType.parse(photo.getMimeType());
                }

                @Override
                public long contentLength() {
                  return inputStreamWrapper.getBytes();
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                  if (isConsumed) {
                    throw new IOException("InputStream has already been consumed");
                  }
                  isConsumed = true;
                  try (Source source = Okio.source(inputStreamWrapper.getStream())) {
                    sink.writeAll(source);
                  }
                }
              };

          MultipartBody.Builder builder =
              new MultipartBody.Builder()
                  .setType(MultipartBody.FORM)
                  .addFormDataPart("item_id", photo.getDataId())
                  .addFormDataPart("title", photo.getTitle())
                  .addFormDataPart("job_id", jobId.toString())
                  .addFormDataPart("service", exportingService);

          String imageDescription = photo.getDescription();
          if (!Strings.isNullOrEmpty(imageDescription)) {
            builder.addFormDataPart("description", imageDescription);
          }
          Date imageUploadedTime = photo.getUploadedTime();
          if (imageUploadedTime != null) {
            long timestampInSeconds = imageUploadedTime.getTime() / 1000;
            builder.addFormDataPart("uploaded_time", String.valueOf(timestampInSeconds));
          }

          builder.addFormDataPart("file_size", String.valueOf(inputStreamWrapper.getBytes()));
          builder.addFormDataPart("file", photo.getTitle(), fileBody);
          return builder.build();
        };

    @SuppressWarnings("unchecked")
    Map<String, Object> responseData =
        (Map<String, Object>)
            sendPostRequest(c2Api.getUploadItem(), bodyGenerator, jobId).get("data");
    monitor.info(
        () ->
            String.format(
                "[SynologyImporter] photo created successfully, name: [%s].", photo.getTitle()),
        jobId);
    return responseData;
  }

  /**
   * Creates video.
   *
   * @param video the video
   * @param jobId the job ID
   * @return a map of shape {"data": {"item_id": <item_id>}}
   */
  public Map<String, Object> createVideo(VideoModel video, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    monitor.info(
        () ->
            String.format(
                "[SynologyImporter] starts creating video, dataId: [%s], name: [%s].",
                video.getDataId(), video.getName()),
        jobId);

    InputStreamWrapper inputStreamWrapper =
        getMediaInputStreamWrapper(jobId, video.getFetchableUrl(), video.isInTempStore());

    int actualChunkCount;
    try (InputStream inputStream = inputStreamWrapper.getStream()) {
      actualChunkCount = uploadVideoChunks(video, jobId, inputStream);
    }

    Map<String, Object> responseData = completeVideoUpload(video, jobId, actualChunkCount);

    monitor.info(
        () ->
            String.format(
                "[SynologyImporter] video created successfully, name: [%s].", video.getName()),
        jobId);
    return responseData;
  }

  private int uploadVideoChunks(VideoModel video, UUID jobId, InputStream inputStream)
      throws CopyExceptionWithFailureReason, IOException {
    final int CHUNK_SIZE = 50 * 1024 * 1024; // 50MB
    int actualChunkCount = 0;

    // reuse the same byte array for each chunk to reduce memory usage,
    // since we are uploading in sequential, there is no concurrency issue
    byte[] chunkData = new byte[CHUNK_SIZE];
    while (true) {
      int bytesRead = inputStream.readNBytes(chunkData, 0, CHUNK_SIZE);
      if (bytesRead == 0) {
        break;
      }

      RequestBody fileBody =
          new RequestBody() {
            @Override
            public MediaType contentType() {
              return MediaType.parse(video.getMimeType());
            }

            @Override
            public long contentLength() {
              return bytesRead;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
              // write the actual bytes read for the last chunk, which may be smaller than
              // CHUNK_SIZE
              sink.write(chunkData, 0, bytesRead);
            }
          };

      MultipartBody.Builder builder =
          new MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart("item_id", video.getDataId())
              .addFormDataPart("index", String.valueOf(actualChunkCount))
              .addFormDataPart("job_id", jobId.toString())
              .addFormDataPart("service", exportingService)
              .addFormDataPart("file_size", String.valueOf(bytesRead))
              .addFormDataPart("file", video.getName(), fileBody);

      RequestBody requestBody = builder.build();
      sendPostRequest(c2Api.getChunkUploadItem(), () -> requestBody, jobId);

      actualChunkCount++;
    }
    return actualChunkCount;
  }

  private Map<String, Object> completeVideoUpload(VideoModel video, UUID jobId, int totalChunks)
      throws CopyExceptionWithFailureReason, IOException {
    FormBody.Builder builder =
        new FormBody.Builder()
            .add("item_id", video.getDataId())
            .add("title", video.getName())
            .add("total_chunks", String.valueOf(totalChunks))
            .add("job_id", jobId.toString())
            .add("service", exportingService);

    if (!Strings.isNullOrEmpty(video.getDescription())) {
      builder.add("description", video.getDescription());
    }
    if (video.getUploadedTime() != null) {
      builder.add("uploaded_time", String.valueOf(video.getUploadedTime().getTime() / 1000));
    }
    RequestBody requestBody = builder.build();

    @SuppressWarnings("unchecked")
    Map<String, Object> responseData =
        (Map<String, Object>)
            sendPostRequest(c2Api.getCompleteUploadItem(), () -> requestBody, jobId).get("data");
    return responseData;
  }

  /**
   * Adds item to album.
   *
   * @param albumId the album ID
   * @param itemId the item ID
   * @return a map of shape {"success": <bool>}
   */
  public Map<String, Object> addItemToAlbum(String albumId, String itemId, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    FormBody.Builder builder =
        new FormBody.Builder()
            .add("job_id", jobId.toString())
            .add("service", exportingService)
            .add("album_id", albumId)
            .add("item_id", itemId);
    RequestBody requestBody = builder.build();
    return sendPostRequest(c2Api.getAddItemToAlbum(), () -> requestBody, jobId);
  }

  /**
   * Updates job status.
   *
   * @param jobStatus the job status
   * @param jobId the job ID
   * @return a map of shape {"success": <bool>}
   */
  public Map<String, Object> sendJobSignal(JobLifeCycle jobStatus, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    FormBody.Builder builder =
        new FormBody.Builder()
            .add("job_id", jobId.toString())
            .add("service", exportingService)
            .add("state", jobStatus.state().name());
    if (jobStatus.endReason() != null) {
      builder.add("end_reason", jobStatus.endReason().name());
    }

    RequestBody requestBody = builder.build();
    return sendPostRequest(c2Api.getSignalJob(), () -> requestBody, jobId);
  }

  @VisibleForTesting
  protected void throwExceptionIfNoQuota(Response response) throws CopyExceptionWithFailureReason {
    String errorCode = "";
    String errorMessage = "";

    try {
      assert response.body() != null;

      String bodyString = response.body().string();
      Map<String, Object> responseData =
          (Map<String, Object>) objectMapper.readValue(bodyString, Map.class);

      if (!responseData.containsKey("error")) {
        return;
      }

      Map<String, Object> errorData = (Map<String, Object>) responseData.get("error");
      errorCode = errorData.getOrDefault("code", "").toString();
      errorMessage = errorData.getOrDefault("msg", "").toString();
    } catch (Exception e) {
      monitor.severe(
          () ->
              "[SynologyImporter] Failed to parse unprocessable content response data, exception:",
          e);
    }

    if (errorCode.equals("2000") || errorCode.equals("2001")) {
      throw new NoNasInAccountException(
          "Synology account has no NAS associated",
          new IOException(
              String.format(
                  "SynologyDTPService get http 422 with error: %s (%s)", errorMessage, errorCode)));
    }
  }

  /*
   * @param url the URL to send the POST request to
   * @param bodyGenerator a generator function that produces the request body, it will be called for each retry attempt
   * @param jobId the job ID
   * @param timeoutInSeconds the timeout for the request in seconds, -1 means do not modify the default timeout of OkHttpClient
   */
  @VisibleForTesting
  protected Map<String, Object> sendPostRequest(
      String url, RequestBodyGenerator bodyGenerator, UUID jobId)
      throws CopyExceptionWithFailureReason, IOException {
    boolean triedRefreshToken = false;

    Exception lastException = null;
    for (int retry = retryConfig.getMaxAttempts(); retry > 0; retry--) {
      final String methodInfo =
          String.format(
              "[SynologyImporter] Sending POST request to url: [%s], attempts left: [%d]",
              url, retry);
      monitor.info(() -> methodInfo, jobId);
      Response response = null;
      try {
        RequestBody body = bodyGenerator.get();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        requestBuilder.header("Authorization", "Bearer " + tokenManager.getAccessToken(jobId));
        response = client.newCall(requestBuilder.build()).execute();
        if (!response.isSuccessful()) {
          int code = response.code();
          if (code == 401) {
            if (triedRefreshToken) {
              throw new InvalidTokenException(
                  "Synology access token is invalid even after refresh",
                  new IOException("SynologyDTPService get http 401 unauthorized"));
            }
            triedRefreshToken = true;
            if (tokenManager.refreshToken(jobId, client, objectMapper)) {
              continue;
            }
          }

          if (code == 413) {
            throw new DestinationMemoryFullException(
                "Synology destination storage limit reached",
                new IOException("SynologyDTPService get http 413 content too large"));

          } else if (code == 422) {
            throwExceptionIfNoQuota(response);
          }

          throw new IOException(
              String.format(
                  "SynologyDTPService get http %d with error: %s", code, response.message()));
        }
      } catch (CopyExceptionWithFailureReason e) {
        monitor.severe(
            () -> "[SynologyImporter] Failed to send post request,", "url:", url, "exception:", e);
        throw e;
      } catch (Exception e) {
        monitor.severe(
            () -> "[SynologyImporter] Failed to send post request,", "url:", url, "exception:", e);
        if (response != null) {
          response.close();
        }
        lastException = e;
        continue;
      }

      try {
        assert response.body() != null;
        String bodyString = response.body().string();
        Map<String, Object> responseData =
            (Map<String, Object>) objectMapper.readValue(bodyString, Map.class);
        return responseData;
      } catch (Exception e) {
        monitor.severe(
            () -> "[SynologyImporter] Failed to parse response data,",
            "url:",
            url,
            "exception:",
            e);
        lastException = e;
      }
    }
    throw new IOException(
        String.format(
            "Failed to send POST request after %d retries: %s",
            retryConfig.getMaxAttempts(), lastException.getMessage()),
        lastException);
  }
}
