/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.photos;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.NewMediaRequest;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.jetbrains.annotations.NotNull;

/**
 * An Apple importer to import the Videos into Apple iCloud-photos.
 */
public class AppleVideosImporter extends AppleInterfaceFactory
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {
  public static final String BYTES_KEY = "bytes";
  public static final String VIDEO_COUNT_KEY = "videosCount";
  private final AppCredentials appCredentials;
  private final String exportingService;
  private final Monitor monitor;
  private final AppleInterfaceFactory factory;

  public AppleVideosImporter(
      @NotNull final AppCredentials appCredentials, @NotNull final Monitor monitor) {
    this(appCredentials, JobMetadata.getExportService(), monitor, new AppleInterfaceFactory());
  }

  @VisibleForTesting
  AppleVideosImporter(
      @NotNull final AppCredentials appCredentials,
      @NotNull final String exportingService,
      @NotNull final Monitor monitor,
      @NotNull final AppleInterfaceFactory factory) {
    this.appCredentials = appCredentials;
    this.exportingService = exportingService;
    this.monitor = monitor;
    this.factory = factory;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      VideosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    long bytes = 0L;
    int videoCount = 0;

    final Collection<VideoModel> videos = data.getVideos();

    if (videos != null && videos.size() > 0) {
      List<VideoModel> newVideos =
          videos.stream()
              .filter(video -> !idempotentExecutor.isKeyCached(getIdempotentId(video)))
              .collect(Collectors.toList());

      UnmodifiableIterator<List<VideoModel>> batches =
          Iterators.partition(newVideos.iterator(), ApplePhotosConstants.maxNewMediaRequests);
      while (batches.hasNext()) {
        final Map<String, Long> batchImportResults =
            importVideoBatch(jobId, authData, batches.next(), idempotentExecutor);
        bytes += batchImportResults.get(BYTES_KEY);
        videoCount += batchImportResults.get(VIDEO_COUNT_KEY);
      }
    }
    final ImportResult result = ImportResult.OK;
    final Map<String, Integer> counts =
        new ImmutableMap.Builder<String, Integer>().put(VIDEO_COUNT_KEY, videoCount).build();
    return result.copyWithBytes(bytes).copyWithCounts(counts);
  }

  // return {BYTES_KEY: Long, VIDEO_COUNT_KEY: Long}
  private Map<String, Long> importVideoBatch(
      UUID jobId,
      TokensAndUrlAuthData authData,
      List<VideoModel> videos,
      IdempotentImportExecutor idempotentImportExecutor)
      throws Exception {
    AppleMediaInterface mediaInterface = getMediaInterface(authData);
    final Map<String, VideoModel> dataIdToVideoModel =
        videos.stream().collect(Collectors.toMap(VideoModel::getDataId, VideoModel -> VideoModel));
    // get upload url
    final PhotosProtocol.GetUploadUrlsResponse getUploadUrlsResponse =
        mediaInterface.getUploadUrl(
            jobId.toString(),
            ApplePhotosConstants.AppleMediaType.VIDEO.toString(),
            videos.stream().map(VideoModel::getDataId).collect(Collectors.toList()));
    final List<PhotosProtocol.AuthorizeUploadResponse> successAuthorizeUploadResponseList =
        new ArrayList<>();
    for (PhotosProtocol.AuthorizeUploadResponse authorizeUploadResponse :
        getUploadUrlsResponse.getUrlResponsesList()) {
      final String dataId = authorizeUploadResponse.getDataId();
      if (authorizeUploadResponse.hasStatus()
          && authorizeUploadResponse.getStatus().getCode() == SC_OK) {
        successAuthorizeUploadResponseList.add(authorizeUploadResponse);
      } else {
        // collect errors in get upload url
        final VideoModel VideoModel = dataIdToVideoModel.get(dataId);
        idempotentImportExecutor.executeAndSwallowIOExceptions(
          getIdempotentId(VideoModel),
          VideoModel.getName(),
          () -> {
            monitor.severe(
              () -> "Fail to get upload url: ",
              AuditKeys.jobId,
              jobId,
              AuditKeys.dataId,
              VideoModel.getDataId(),
              AuditKeys.albumId,
              VideoModel.getAlbumId(),
              AuditKeys.statusCode,
              authorizeUploadResponse.getStatus().getCode());
            throw new IOException(
              String.format(
                "Fail to get upload url, error code: %d",
                authorizeUploadResponse.getStatus().getCode()));
          });
      }
    }

    // download then upload content
    final Map<String, String> dataIdToUploadResponse =
        mediaInterface.uploadContent(
            dataIdToVideoModel.values().stream()
                .collect(
                    Collectors.toMap(
                        VideoModel::getDataId,
                        VideoModel -> VideoModel.getContentUrl().toString())),
            successAuthorizeUploadResponseList);

    // collect errors in upload content
    for (PhotosProtocol.AuthorizeUploadResponse authorizeUploadResponse :
        successAuthorizeUploadResponseList) {
      final String dataId = authorizeUploadResponse.getDataId();
      if (!dataIdToUploadResponse.containsKey(dataId)) {
        final VideoModel VideoModel = dataIdToVideoModel.get(dataId);
        idempotentImportExecutor.executeAndSwallowIOExceptions(
            getIdempotentId(VideoModel),
            VideoModel.getName(),
            () -> {
              monitor.severe(
                  () -> "Fail to upload content: ",
                  AuditKeys.jobId,
                  jobId,
                  AuditKeys.dataId,
                  VideoModel.getDataId(),
                  AuditKeys.albumId,
                  VideoModel.getAlbumId());
              throw new IOException("Fail to upload content");
            });
      }
    }

    final List<NewMediaRequest> newMediaRequestList = getNewMediaRequests(
      dataIdToVideoModel, dataIdToUploadResponse);

    final PhotosProtocol.CreateMediaResponse createMediaResponse =
        mediaInterface.createMedia(jobId.toString(),
          ApplePhotosConstants.AppleMediaType.VIDEO.toString(),
          newMediaRequestList);

    // collect results in create media
    Pair<Long, Long> pair = processResults(createMediaResponse,
      dataIdToVideoModel,
      idempotentImportExecutor,
      jobId);

    // return count and bytes
    monitor.info(
        () -> "Apple imported video batch",
        AuditKeys.jobId,
        jobId,
        AuditKeys.totalFilesCount,
        pair.getKey(),
        AuditKeys.bytesExported,
        pair.getValue());

    return new ImmutableMap.Builder<String, Long>()
            .put(VIDEO_COUNT_KEY, pair.getKey())
            .put(BYTES_KEY, pair.getValue())
            .build();
  }


  @NotNull
  private Pair<Long, Long> processResults(PhotosProtocol.CreateMediaResponse createMediaResponse,
    Map<String, VideoModel> dataIdToVideoModel,
    IdempotentImportExecutor idempotentImportExecutor,
    UUID jobId) throws Exception {
    long totalBytes = 0L;
    long videoCount = 0;
    for (PhotosProtocol.NewMediaResponse newMediaResponse :
      createMediaResponse.getNewMediaResponsesList()) {
      final String dataId = newMediaResponse.getDataId();
      final VideoModel VideoModel = dataIdToVideoModel.get(dataId);
      if (newMediaResponse.hasStatus()
        && newMediaResponse.getStatus().getCode() == SC_OK) {
        videoCount += 1;
        totalBytes += newMediaResponse.getFilesize();
        idempotentImportExecutor.executeAndSwallowIOExceptions(
          getIdempotentId(VideoModel),
          VideoModel.getName(),
          () -> {
            monitor.debug(
              () -> "Apple importing video",
              AuditKeys.jobId,
              jobId,
              AuditKeys.dataId,
              VideoModel.getDataId(),
              AuditKeys.albumId,
              VideoModel.getAlbumId(),
              AuditKeys.recordId,
              newMediaResponse.getRecordId());
            return newMediaResponse.getRecordId();
          });
      } else if (newMediaResponse.getStatus().getCode() == SC_CONFLICT) {
        idempotentImportExecutor.executeAndSwallowIOExceptions(
          getIdempotentId(VideoModel),
          VideoModel.getName(),
          () -> {
            monitor.debug(
              () -> "duplicated video",
              AuditKeys.jobId,
              jobId,
              AuditKeys.dataId,
              VideoModel.getDataId(),
              AuditKeys.albumId,
              VideoModel.getAlbumId(),
              AuditKeys.recordId,
              newMediaResponse.getRecordId());
            return newMediaResponse.getRecordId();
          });
      } else {
        idempotentImportExecutor.executeAndSwallowIOExceptions(
          getIdempotentId(VideoModel),
          VideoModel.getName(),
          () -> {
            monitor.severe(
              () -> "Fail to create media: ",
              AuditKeys.jobId,
              jobId,
              AuditKeys.dataId,
              VideoModel.getDataId(),
              AuditKeys.albumId,
              VideoModel.getAlbumId(),
              AuditKeys.statusCode,
              newMediaResponse.getStatus().getCode());
            throw new IOException(
              String.format(
                "Fail to create media, error code: %d",
                newMediaResponse.getStatus().getCode()));
          });
      }
    }
    return Pair.of(videoCount, totalBytes);
  }
  @NotNull
  private List<NewMediaRequest> getNewMediaRequests(Map<String, VideoModel> dataIdToVideoModel,
    Map<String, String> dataIdToUploadResponse) {
    final List<NewMediaRequest> newMediaRequestList = new ArrayList<>();
    for (String dataId : dataIdToUploadResponse.keySet()) {
      final VideoModel VideoModel = dataIdToVideoModel.get(dataId);
      final String singleFileUploadResponse =
          dataIdToUploadResponse.get(dataId);
      String filename = VideoModel.getName();
      String description = VideoModel.getDescription();
      String mediaType = VideoModel.getEncodingFormat();
      String albumId = VideoModel.getAlbumId();
      Long creationDateInMillis = System.currentTimeMillis();
      newMediaRequestList.add(
          AppleMediaInterface.createNewMediaRequest(
              dataId,
              filename,
              description,
              albumId,
              mediaType,
              null,
              creationDateInMillis,
              singleFileUploadResponse));
    }
    return newMediaRequestList;
  }

  private String getIdempotentId(VideoModel video) {
    return Optional.ofNullable(video.getAlbumId())
      .map(a -> a + "-" + video.getDataId())
      .orElse(video.getDataId());
  }

  private AppleMediaInterface getMediaInterface(TokensAndUrlAuthData authData) {
    return factory.getOrCreateMediaInterface(authData, appCredentials,
      exportingService, monitor);
  }
}
