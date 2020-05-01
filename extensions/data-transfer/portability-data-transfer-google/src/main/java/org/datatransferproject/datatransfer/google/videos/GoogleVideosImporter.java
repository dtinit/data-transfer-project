/*
 * Copyright 2019 The Data Transfer Project Authors.
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

/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.google.videos;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.upload.UploadMediaItemResponse.Error;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.rpc.Code;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  // TODO: internationalize copy prefix
  private static final String COPY_PREFIX = "Copy of ";

  private final ImageStreamProvider videoStreamProvider;
  private Monitor monitor;
  private final AppCredentials appCredentials;
  private final TemporaryPerJobDataStore dataStore;

  public GoogleVideosImporter(
      AppCredentials appCredentials, TemporaryPerJobDataStore dataStore, Monitor monitor) {
    this(new ImageStreamProvider(), monitor, appCredentials, dataStore);
  }

  @VisibleForTesting
  GoogleVideosImporter(
      ImageStreamProvider videoStreamProvider,
      Monitor monitor,
      AppCredentials appCredentials,
      TemporaryPerJobDataStore dataStore) {
    this.videoStreamProvider = videoStreamProvider;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.dataStore = dataStore;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor executor,
      TokensAndUrlAuthData authData,
      VideosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    PhotosLibrarySettings settings =
        PhotosLibrarySettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider.create(
                    UserCredentials.newBuilder()
                        .setClientId(appCredentials.getKey())
                        .setClientSecret(appCredentials.getSecret())
                        .setAccessToken(new AccessToken(authData.getAccessToken(), null))
                        .setRefreshToken(authData.getRefreshToken())
                        .build()))
            .build();

    long bytes = 0L;
    //     Uploads videos
    if (data.getVideos() != null && data.getVideos().size() > 0) {
      for (VideoObject video : data.getVideos()) {
        final VideoResult result =
            executor.executeAndSwallowIOExceptions(
                video.getDataId(), video.getName(), () -> importSingleVideo(video, settings));
        if (result != null) {
          bytes += result.getBytes();
        }
      }
    }
    final ImportResult result = ImportResult.OK;
    return result.copyWithBytes(bytes);
  }

  VideoResult importSingleVideo(VideoObject inputVideo, PhotosLibrarySettings settings)
      throws Exception {
    if (inputVideo.getContentUrl() == null) {
      monitor.info(() -> "Content Url is empty. Make sure that you provide a valid content Url.");
      return null;
    }

    String filename;
    if (Strings.isNullOrEmpty(inputVideo.getName())) {
      filename = "untitled";
    } else {
      filename = COPY_PREFIX + inputVideo.getName();
    }

    final File tmp;
    HttpURLConnection conn =
        this.videoStreamProvider.getConnection(inputVideo.getContentUrl().toString());
    try (InputStream inputStream = conn.getInputStream()) {
      tmp = dataStore.getTempFileFromInputStream(inputStream, filename, ".mp4");
    }

    try (PhotosLibraryClient photosLibraryClient = PhotosLibraryClient.initialize(settings)) {
      UploadMediaItemRequest uploadRequest =
          UploadMediaItemRequest.newBuilder()
              .setFileName(filename)
              .setDataFile(new RandomAccessFile(tmp, "r"))
              .build();
      UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);
      if (uploadResponse.getError().isPresent() || !uploadResponse.getUploadToken().isPresent()) {
        Error error = uploadResponse.getError().orElse(null);
        if (error != null
            && error
                .getCause()
                .getMessage()
                .contains("The upload url is either finalized or rejected by the server")) {
          throw new UploadErrorException(
              "Upload was terminated because of error", error.getCause());
        }
        throw new IOException(
            "An error was encountered while uploading the video.",
            error != null ? error.getCause() : null);
      } else {
        String uploadToken = uploadResponse.getUploadToken().get();
        return new VideoResult(
            createMediaItem(inputVideo, photosLibraryClient, uploadToken), tmp.length());
      }
    } catch (InvalidArgumentException e) {
      if (e.getMessage().contains("The remaining storage in the user's account is not enough")) {
        throw new DestinationMemoryFullException("Google destination storage full", e);
      } else {
        throw e;
      }
    } finally {
      //noinspection ResultOfMethodCallIgnored
      tmp.delete();
    }
  }

  String createMediaItem(
      VideoObject inputVideo, PhotosLibraryClient photosLibraryClient, String uploadToken)
      throws IOException {
    NewMediaItem newMediaItem;
    if (inputVideo.getDescription() != null && !inputVideo.getDescription().isEmpty()) {
      newMediaItem =
          NewMediaItemFactory.createNewMediaItem(uploadToken, inputVideo.getDescription());
    } else {
      newMediaItem = NewMediaItemFactory.createNewMediaItem(uploadToken);
    }

    List<NewMediaItem> newItems = Collections.singletonList(newMediaItem);

    BatchCreateMediaItemsResponse response = photosLibraryClient.batchCreateMediaItems(newItems);
    final List<NewMediaItemResult> resultsList = response.getNewMediaItemResultsList();
    if (resultsList.size() != 1) {
      throw new IOException("Expected resultsList to be of size 1");
    } else {
      final NewMediaItemResult itemResult = resultsList.get(0);
      final int code = itemResult.getStatus().getCode();
      if (code != Code.OK_VALUE) {
        throw new IOException(
            String.format(
                "Video item could not be created. Code: %d Message: %s",
                code, itemResult.getStatus().getMessage()));
      } else {
        return itemResult.getMediaItem().getId();
      }
    }
  }
}
