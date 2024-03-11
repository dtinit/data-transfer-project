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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static java.lang.String.format;

/**
 * An Apple importer to import the Photos and Videos into Apple iCloud-photos.
 */
public class AppleMediaImporter implements Importer<TokensAndUrlAuthData, MediaContainerResource> {
  private final AppCredentials appCredentials;
  private final String exportingService;
  private final Monitor monitor;
  private final AppleInterfaceFactory factory;
  private IdempotentImportExecutor retryingIdempotentExecutor;
  private Boolean enableRetrying;

  public AppleMediaImporter(
      @NotNull final AppCredentials appCredentials, @NotNull final Monitor monitor, @Nullable IdempotentImportExecutor retryingIdempotentExecutor, boolean enableRetrying) {
    this(appCredentials, JobMetadata.getExportService(), monitor, new AppleInterfaceFactory(), retryingIdempotentExecutor, enableRetrying);
  }

  @VisibleForTesting
  AppleMediaImporter(
    @NotNull final AppCredentials appCredentials, @NotNull  String exportingService,
    @NotNull final Monitor monitor, @NotNull  AppleInterfaceFactory factory,
    @Nullable IdempotentImportExecutor retryingIdempotentExecutor,
    boolean enableRetrying) {
    this.appCredentials = appCredentials;
    this.exportingService = exportingService;
    this.monitor = monitor;
    this.factory = factory;
    this.retryingIdempotentExecutor = retryingIdempotentExecutor;
    this.enableRetrying = enableRetrying;
  }
  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      MediaContainerResource data)
      throws Exception {
    if (data == null) {
      return ImportResult.OK;
    }

    // log
    for (PhotoModel photoModel: data.getPhotos()) {
      monitor.info(() -> "AppleMediaImporter received data",
              AuditKeys.dataId, photoModel.getDataId(),
              AuditKeys.updatedTimeInMs, photoModel.getUploadedTime());
    }

    for (VideoModel videoModel: data.getVideos()) {
      monitor.info(() -> "AppleMediaImporter received data",
              AuditKeys.dataId, videoModel.getDataId(),
              AuditKeys.updatedTimeInMs, videoModel.getUploadedTime());
    }

    IdempotentImportExecutor executor =
            (retryingIdempotentExecutor != null && enableRetrying) ? retryingIdempotentExecutor : idempotentExecutor;

    AppleMediaInterface mediaInterface = factory
      .getOrCreateMediaInterface(jobId, authData, appCredentials, exportingService, monitor);

    final String retryId = format("AppleMediaImporter_%s_%s", jobId, UUID.randomUUID());

    final Map<String, Long> importPhotosMap = new HashMap<>();
    final Map<String, Long> importVideosResult = new HashMap<>();
    final Map<String, Integer> counts = new HashMap<>();

    // lower stack retry logic in data copier will not handle the skippable error case, that's why we want to build retry logic in importer itself.

    // executor can either be a RetryingInMemoryIdempotentImportExecutor or an InMemoryIdempotentImportExecutor
    // RetryingInMemoryIdempotentImportExecutor will return null for skippable error, throw for the others
    // InMemoryIdempotentImportExecutor will throw every error (the callableImporter will throw it as well if we don't throw it here)

    // if executor is a RetryingInMemoryIdempotentImportExecutor, then it will be different from the idempotentExecutor in the param, which will check for recent errors in lower stack (CallableImporter),
    // So if the error in executor is skippable, we need to clean the errors in idempotentExecutor to make sure they will not be thrown in the lower stack.

    executor.executeOrThrowException(retryId, retryId, () -> {
      importPhotosMap.clear();
      importVideosResult.clear();
      counts.clear();
      idempotentExecutor.resetRecentErrors();

      final int albumCount =
              mediaInterface.importAlbums(
                      jobId,
                      idempotentExecutor,
                      data.getAlbums(),
                      DataVertical.MEDIA.getDataType());
      importPhotosMap.putAll(
              mediaInterface.importAllMedia(
                      jobId,
                      idempotentExecutor,
                      data.getPhotos(),
                      DataVertical.MEDIA.getDataType()));
      importVideosResult.putAll(
              mediaInterface.importAllMedia(
                      jobId,
                      idempotentExecutor,
                      data.getVideos(),
                      DataVertical.MEDIA.getDataType()));
      counts.putAll(
              new ImmutableMap.Builder<String, Integer>()
                      .put(MediaContainerResource.ALBUMS_COUNT_DATA_NAME, albumCount)
                      .put(
                              MediaContainerResource.PHOTOS_COUNT_DATA_NAME,
                              importPhotosMap.getOrDefault(ApplePhotosConstants.COUNT_KEY, 0L).intValue())
                      .put(
                              MediaContainerResource.VIDEOS_COUNT_DATA_NAME,
                              importVideosResult.getOrDefault(ApplePhotosConstants.COUNT_KEY, 0L).intValue())
                      .build());

      Collection<ErrorDetail> errors = idempotentExecutor.getRecentErrors();
      if (!errors.isEmpty() && executor instanceof RetryingInMemoryIdempotentImportExecutor) { // throw the error for retryExecutor to retry, only include the actual error message, but not the stack traces
        throw new IOException(errors.iterator().hasNext() ? errors.iterator().next().exception().lines().findFirst().get() : ApplePhotosConstants.APPLE_PHOTOS_IMPORT_ERROR_PREFIX + " Unknown Error");
      }
      return true;
    });

    // if retryingExecutor thinks the errors are skippable, we need to clean the errors in idempotentExecutor to make sure they will not be thrown in the lower stack.
    // (Notice that lower stack CallableImporter does not have skip logic)
    if(executor instanceof RetryingInMemoryIdempotentImportExecutor) {
      Collection<ErrorDetail> recentErrorsFromRetryingExecutor = executor.getRecentErrors();
      if (!recentErrorsFromRetryingExecutor.isEmpty() && recentErrorsFromRetryingExecutor.iterator().hasNext() && recentErrorsFromRetryingExecutor.stream().allMatch(errorDetail -> errorDetail.canSkip())) {
        idempotentExecutor.resetRecentErrors();
      }
    }

    return ImportResult.OK
        .copyWithBytes(
            importPhotosMap.getOrDefault(ApplePhotosConstants.BYTES_KEY, 0L)
                + importVideosResult.getOrDefault(ApplePhotosConstants.BYTES_KEY, 0L))
        .copyWithCounts(counts);
  }
}
