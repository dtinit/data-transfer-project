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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
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
import com.google.rpc.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.MediaObject;
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
  private Map<UUID, PhotosLibraryClient> clientsMap = new HashMap<>();

  public GoogleVideosImporter(
      AppCredentials appCredentials, TemporaryPerJobDataStore dataStore, Monitor monitor) {
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.dataStore = dataStore;
    this.videoStreamProvider = new ImageStreamProvider();
  }

  GoogleVideosImporter(
      AppCredentials appCredentials,
      TemporaryPerJobDataStore dataStore,
      Monitor monitor,
      ImageStreamProvider videoStreamProvider) {
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

    PhotosLibraryClient client;
    if (clientsMap.containsKey(jobId)) {
      client = clientsMap.get(jobId);
    } else {
      PhotosLibrarySettings settings =
          PhotosLibrarySettings.newBuilder()
              .setCredentialsProvider(
                  FixedCredentialsProvider.create(
                      UserCredentials.newBuilder()
                          .setClientId(appCredentials.getKey())
                          .setClientSecret(appCredentials.getSecret())
                          .setAccessToken(new AccessToken(authData.getAccessToken(), new Date()))
                          .setRefreshToken(authData.getRefreshToken())
                          .build()))
              .build();
      client = PhotosLibraryClient.initialize(settings);
      clientsMap.put(jobId, client);
    }

    long bytes = 0L;
    //     Uploads videos
    final Collection<VideoObject> videos = data.getVideos();
    if (videos != null && videos.size() > 0) {
      Stream<VideoObject> stream =
          videos.stream()
              .filter(video -> shouldImport(video, executor))
              .map(this::transformVideoName);
      // We partition into groups of 49 as 50 is the maximum number of items that can be created in
      // one call. (We use 49 to avoid potential off by one errors)
      // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
      final UnmodifiableIterator<List<VideoObject>> batches =
          Iterators.partition(stream.iterator(), 49);
      while (batches.hasNext()) {
        long batchBytes = importVideoBatch(batches.next(), client, executor);
        bytes += batchBytes;
      }
    }
    final ImportResult result = ImportResult.OK;
    return result.copyWithBytes(bytes);
  }

  private boolean shouldImport(VideoObject video, IdempotentImportExecutor executor) {
    if (video.getContentUrl() == null) {
      monitor.info(() -> "Content Url is empty. Make sure that you provide a valid content Url.");
      return false;
    } else {
      // If the video key is already cached there is no need to retry.
      return !executor.isKeyCached(video.getDataId());
    }
  }

  private VideoObject transformVideoName(VideoObject video) {
    String filename;
    if (Strings.isNullOrEmpty(video.getName())) {
      filename = "untitled";
    } else {
      filename = COPY_PREFIX + video.getName();
    }
    video.setName(filename);
    return video;
  }

  long importVideoBatch(
      List<VideoObject> batchedVideos,
      PhotosLibraryClient client,
      IdempotentImportExecutor executor)
      throws Exception {
    final ArrayList<NewMediaItem> mediaItems = new ArrayList<>();
    final HashMap<String, VideoObject> uploadTokenToDataId = new HashMap<>();
    final HashMap<String, Long> uploadTokenToLength = new HashMap<>();
    // The PhotosLibraryClient can throw InvalidArgumentException and this try block wraps the two
    // calls of the client to handle the InvalidArgumentException when the user's storage is full.
    try {
      for (VideoObject video : batchedVideos) {
        try {
          Pair<String, Long> pair = uploadMediaItem(video, client);
          final String uploadToken = pair.getLeft();
          mediaItems.add(buildMediaItem(video, uploadToken));
          uploadTokenToDataId.put(uploadToken, video);
          uploadTokenToLength.put(uploadToken, pair.getRight());
        } catch (IOException e) {
          if (e instanceof FileNotFoundException) {
            // If the video file is no longer available then skip the video. We see this in a small
            // number of videos where the video has been deleted.
            monitor.info(
                () -> String.format("Video resource was missing for id: %s", video.getDataId()), e);
            continue;
          }
          executor.executeAndSwallowIOExceptions(
              video.getDataId(),
              video.getName(),
              () -> {
                throw e;
              });
        }
      }
      if (mediaItems.isEmpty()) {
        // Either we were not passed in any videos or we failed upload on all of them.
        return 0L;
      }

      BatchCreateMediaItemsResponse response = client.batchCreateMediaItems(mediaItems);
      final List<NewMediaItemResult> resultsList = response.getNewMediaItemResultsList();
      long bytes = 0L;
      for (NewMediaItemResult result : resultsList) {
        String uploadToken = result.getUploadToken();
        Status status = result.getStatus();

        final VideoObject video = uploadTokenToDataId.get(uploadToken);
        Preconditions.checkNotNull(video);
        final int code = status.getCode();
        if (code == Code.OK_VALUE) {
          executor.executeAndSwallowIOExceptions(
              video.getDataId(), video.getName(), () -> result.getMediaItem().getId());
          Long length = uploadTokenToLength.get(uploadToken);
          if (length != null) {
            bytes += length;
          }
        } else {
          executor.executeAndSwallowIOExceptions(
              video.getDataId(),
              video.getName(),
              () -> {
                throw new IOException(
                    String.format(
                        "Video item could not be created. Code: %d Message: %s",
                        code, result.getStatus().getMessage()));
              });
        }
        uploadTokenToDataId.remove(uploadToken);
      }
      if (!uploadTokenToDataId.isEmpty()) {
        for (VideoObject video : uploadTokenToDataId.values()) {
          executor.executeAndSwallowIOExceptions(
              video.getDataId(),
              video.getName(),
              () -> {
                throw new IOException("Video item was missing from results list.");
              });
        }
      }
      return bytes;
    } catch (InvalidArgumentException e) {
      if (e.getMessage().contains("The remaining storage in the user's account is not enough")) {
        throw new DestinationMemoryFullException("Google destination storage full", e);
      } else {
        throw e;
      }
    }
  }

  private Pair<String, Long> uploadMediaItem(
      MediaObject inputVideo, PhotosLibraryClient photosLibraryClient)
      throws IOException, UploadErrorException {

    final File tmp;
    try (InputStream inputStream =
        this.videoStreamProvider
            .getConnection(inputVideo.getContentUrl().toString())
            .getInputStream()) {
      tmp = dataStore.getTempFileFromInputStream(inputStream, inputVideo.getName(), ".mp4");
    }
    try {
      UploadMediaItemRequest uploadRequest =
          UploadMediaItemRequest.newBuilder()
              .setFileName(inputVideo.getName())
              .setDataFile(new RandomAccessFile(tmp, "r"))
              .build();
      UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);
      String uploadToken;
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
        uploadToken = uploadResponse.getUploadToken().get();
      }
      return Pair.of(uploadToken, tmp.length());
    } finally {
      //noinspection ResultOfMethodCallIgnored
      tmp.delete();
    }
  }

  @VisibleForTesting
  NewMediaItem buildMediaItem(VideoObject inputVideo, String uploadToken) {
    NewMediaItem newMediaItem;
    String videoDescription = inputVideo.getDescription();
    if (Strings.isNullOrEmpty(videoDescription)) {
      newMediaItem = NewMediaItemFactory.createNewMediaItem(uploadToken);
    } else {
      if (videoDescription.length() > 1000) {
        videoDescription = videoDescription.substring(0, 997) + "...";
      }
      newMediaItem = NewMediaItemFactory.createNewMediaItem(uploadToken, videoDescription);
    }
    return newMediaItem;
  }
}
