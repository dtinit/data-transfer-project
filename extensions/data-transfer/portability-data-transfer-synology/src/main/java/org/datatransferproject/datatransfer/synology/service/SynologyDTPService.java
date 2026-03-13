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
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.models.C2Api;
import org.datatransferproject.datatransfer.synology.models.ServiceConfig;
import org.datatransferproject.datatransfer.synology.utils.ServiceConfigParser;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.NoNasInAccountException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
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
    this.client = client;
  }

  /**
   * getInputStream
   *
   * @param url
   * @return an InputStream Instance
   * @throws IOException
   */
  protected InputStream getInputStream(String url) throws IOException {
    return new URL(url).openStream();
  }

  /**
   * Creates album.
   *
   * @param album the album
   * @param jobId the job ID
   * @return a map of shape {"data": {"album_id": <album_id>}}
   */
  public Map<String, Object> createAlbum(MediaAlbum album, UUID jobId)
      throws CopyExceptionWithFailureReason {
    FormBody.Builder builder = new FormBody.Builder().add("title", album.getName());
    builder.add("album_id", album.getId());
    builder.add("job_id", jobId.toString());
    builder.add("service", exportingService);
    monitor.info(() -> "[SynologyImporter] Creating album", album.getName(), jobId);

    return (Map<String, Object>)
        sendPostRequest(c2Api.getCreateAlbum(), builder.build(), jobId).get("data");
  }

  /**
   * Creates photo.
   *
   * @param photo the photo
   * @param jobId the job ID
   * @return a map of shape {"data": {"item_id": <item_id>}}
   */
  public Map<String, Object> createPhoto(PhotoModel photo, UUID jobId)
      throws CopyExceptionWithFailureReason {
    byte[] imageBytes;
    try {
      InputStream inputStream = null;
      if (photo.isInTempStore()) {
        inputStream = jobStore.getStream(jobId, photo.getFetchableUrl()).getStream();
      } else if (photo.getFetchableUrl() != null) {
        inputStream = getInputStream(photo.getFetchableUrl());
      } else {
        monitor.severe(() -> "[SynologyImporter] Can't get inputStream for a photo");
        return null;
      }
      imageBytes = ByteStreams.toByteArray(inputStream);
    } catch (MalformedURLException e) {
      throw new UploadErrorException("Failed to create url for photo", e);
    } catch (IOException e) {
      throw new UploadErrorException("Failed to create input stream for photo", e);
    }

    RequestBody fileBody = RequestBody.create(MediaType.parse(photo.getMimeType()), imageBytes);

    MultipartBody.Builder builder =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", photo.getTitle(), fileBody)
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

    @SuppressWarnings("unchecked")
    Map<String, Object> responseData =
        (Map<String, Object>)
            sendPostRequest(c2Api.getUploadItem(), builder.build(), jobId).get("data");
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
      throws CopyExceptionWithFailureReason {
    byte[] videoBytes;
    try {
      InputStream inputStream = null;
      if (video.isInTempStore()) {
        inputStream = jobStore.getStream(jobId, video.getFetchableUrl()).getStream();
      } else if (video.getFetchableUrl() != null) {
        inputStream = getInputStream(video.getFetchableUrl());
      } else {
        monitor.severe(() -> "[SynologyImporter] Can't get inputStream for a video");
        return null;
      }

      videoBytes = ByteStreams.toByteArray(inputStream);
    } catch (MalformedURLException e) {
      throw new UploadErrorException("Failed to create url for video", e);
    } catch (IOException e) {
      throw new UploadErrorException("Failed to create input stream for video", e);
    }

    RequestBody fileBody = RequestBody.create(MediaType.parse(video.getMimeType()), videoBytes);
    MultipartBody.Builder builder =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", video.getName(), fileBody)
            .addFormDataPart("item_id", video.getDataId())
            .addFormDataPart("title", video.getName())
            .addFormDataPart("job_id", jobId.toString())
            .addFormDataPart("service", exportingService);

    String imageDescription = video.getDescription();
    if (!Strings.isNullOrEmpty(imageDescription)) {
      builder.addFormDataPart("description", imageDescription);
    }
    Date videoUploadedTime = video.getUploadedTime();
    if (videoUploadedTime != null) {
      long timestampInSeconds = videoUploadedTime.getTime() / 1000;
      builder.addFormDataPart("uploaded_time", String.valueOf(timestampInSeconds));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> responseData =
        (Map<String, Object>)
            sendPostRequest(c2Api.getUploadItem(), builder.build(), jobId).get("data");
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
      throws CopyExceptionWithFailureReason {
    FormBody.Builder builder =
        new FormBody.Builder()
            .add("job_id", jobId.toString())
            .add("service", exportingService)
            .add("album_id", albumId)
            .add("item_id", itemId);
    return sendPostRequest(c2Api.getAddItemToAlbum(), builder.build(), jobId);
  }

  /**
   * Updates job status.
   *
   * @param jobStatus the job status
   * @param jobId the job ID
   * @return a map of shape {"success": <bool>}
   */
  public Map<String, Object> sendJobSignal(JobLifeCycle jobStatus, UUID jobId)
      throws CopyExceptionWithFailureReason {
    FormBody.Builder builder =
        new FormBody.Builder()
            .add("job_id", jobId.toString())
            .add("service", exportingService)
            .add("state", jobStatus.state().name());
    if (jobStatus.endReason() != null) {
      builder.add("end_reason", jobStatus.endReason().name());
    }

    return sendPostRequest(c2Api.getSignalJob(), builder.build(), jobId);
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

  @VisibleForTesting
  protected Map<String, Object> sendPostRequest(String url, RequestBody body, UUID jobId)
      throws CopyExceptionWithFailureReason {
    boolean triedRefreshToken = false;
    Request.Builder requestBuilder = new Request.Builder().url(url).post(body);

    Exception lastException = null;
    for (int retry = retryConfig.getMaxAttempts(); retry > 0; retry--) {
      Response response = null;
      try {
        requestBuilder.header("Authorization", "Bearer " + tokenManager.getAccessToken(jobId));
        response = client.newCall(requestBuilder.build()).execute();
        if (!response.isSuccessful()) {
          int code = response.code();
          if (code == 401 && !triedRefreshToken) {
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
    throw new UploadErrorException(
        String.format(
            "Failed to send POST request after %d retries: %s",
            retryConfig.getMaxAttempts(), lastException.getMessage()),
        lastException);
  }
}
