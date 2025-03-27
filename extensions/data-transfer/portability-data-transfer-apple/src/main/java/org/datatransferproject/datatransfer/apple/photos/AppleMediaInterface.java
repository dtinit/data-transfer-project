package org.datatransferproject.datatransfer.apple.photos;

import com.google.common.collect.ImmutableMap;
import org.datatransferproject.datatransfer.apple.AppleBaseInterface;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.exceptions.AppleContentException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.Iterators;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol;

import static org.apache.http.HttpStatus.SC_OK;
import static org.datatransferproject.datatransfer.apple.photos.ApplePhotosApiClient.getApplePhotosImportThrowingMessage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AppleMediaInterface implements AppleBaseInterface {
  private final ApplePhotosApiClient apiClient;
  private final AppCredentials appCredentials;
  private final String exportingService;
  private final Monitor monitor;
  private final TokensAndUrlAuthData authData;

  public AppleMediaInterface(
          @NotNull final TokensAndUrlAuthData authData,
          @NotNull final AppCredentials appCredentials,
          @NotNull final String exportingService,
          @NotNull final Monitor monitor) {
    this.authData = authData;
    this.appCredentials = appCredentials;
    this.exportingService = exportingService;
    this.monitor = monitor;
    this.apiClient = new ApplePhotosApiClient(authData, appCredentials, monitor);
  }

  public int importAlbums(
          final UUID jobId,
          IdempotentImportExecutor idempotentImportExecutor,
          Collection<MediaAlbum> mediaAlbums,
          @NotNull final String dataClass)
          throws Exception {
    AtomicInteger successAlbumsCount = new AtomicInteger(0);
    final Map<String, MediaAlbum> dataIdToMediaAlbum =
            mediaAlbums.stream().collect(Collectors.toMap(MediaAlbum::getId, mediaAlbum -> mediaAlbum));
    UnmodifiableIterator<List<MediaAlbum>> batches =
            Iterators.partition(mediaAlbums.iterator(), ApplePhotosConstants.maxNewAlbumRequests);
    while (batches.hasNext()) {
      final PhotosProtocol.CreateAlbumsResponse createAlbumsResponse =
              apiClient.createAlbums(jobId.toString(), dataClass, batches.next());
      for (PhotosProtocol.NewPhotoAlbumResponse newPhotoAlbumResponse :
              createAlbumsResponse.getNewPhotoAlbumResponsesList()) {
        final String dataId = newPhotoAlbumResponse.getDataId();
        final MediaAlbum mediaAlbum = dataIdToMediaAlbum.get(dataId);
        if (newPhotoAlbumResponse.hasStatus() && newPhotoAlbumResponse.getStatus().getCode() == SC_OK) {
          successAlbumsCount.getAndIncrement();
          idempotentImportExecutor.executeAndSwallowIOExceptions(
                  mediaAlbum.getId(),
                  mediaAlbum.getName(),
                  () -> {
                    monitor.debug(
                            () -> "Apple importing album",
                            AuditKeys.jobId, jobId,
                            AuditKeys.albumId, dataId,
                            AuditKeys.recordId, newPhotoAlbumResponse.getRecordId());
                    return newPhotoAlbumResponse.getRecordId();
                  });
        } else {
          idempotentImportExecutor.executeAndSwallowIOExceptions(
                  mediaAlbum.getId(),
                  mediaAlbum.getName(),
                  () -> {
                    throw new AppleContentException(getApplePhotosImportThrowingMessage("Fail to create album",
                            ImmutableMap.of(
                                    AuditKeys.errorCode, Optional.of(String.valueOf(newPhotoAlbumResponse.getStatus().getCode())),
                                    AuditKeys.jobId, Optional.of(jobId.toString()),
                                    AuditKeys.albumId, Optional.of(mediaAlbum.getId()))));
                  });
        }
      }
    }
    return successAlbumsCount.get();
  }

  // Note: Methods like importAllMedia, importMediaBatch, uploadContent, etc., would similarly use apiClient
  // and remain in this class, focusing on orchestration and content handling.
}