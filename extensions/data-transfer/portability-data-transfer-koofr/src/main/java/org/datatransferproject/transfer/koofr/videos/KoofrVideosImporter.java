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
package org.datatransferproject.transfer.koofr.videos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.transfer.koofr.KoofrTransmogrificationConfig;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Imports videos and their albums to Koofr.
 */
public class KoofrVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  private final KoofrClientFactory koofrClientFactory;
  private final ConnectionProvider connectionProvider;
  private final Monitor monitor;

  public KoofrVideosImporter(KoofrClientFactory koofrClientFactory, Monitor monitor,
      JobStore jobStore) {
    this.koofrClientFactory = koofrClientFactory;
    this.connectionProvider = new ConnectionProvider(jobStore);
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      VideosContainerResource resource)
      throws Exception {
    KoofrClient koofrClient = koofrClientFactory.create(authData);

    monitor.debug(
        () ->
            String.format(
                "%s: Importing %s albums and %s videos",
                jobId, resource.getAlbums().size(), resource.getVideos().size()));

    // TODO: VideosContainerResource does not support transmogrification

    for (VideoAlbum album : resource.getAlbums()) {
      // Create a Koofr folder and then save the id with the mapping data
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> createAlbumFolder(album, koofrClient));
    }

    final LongAdder totalImportedFilesSizes = new LongAdder();
    for (VideoModel videoModel : resource.getVideos()) {
      idempotentImportExecutor.importAndSwallowIOExceptions(
              videoModel,
           video -> {
             ItemImportResult<String> fileImportResult =
              importSingleVideo(videoModel, jobId, idempotentImportExecutor, koofrClient);
             if (fileImportResult.hasBytes()) {
               totalImportedFilesSizes.add(fileImportResult.getBytes());
             }
             return fileImportResult;
           });
    }
    return ImportResult.OK.copyWithBytes(totalImportedFilesSizes.longValue());
  }

  private String createAlbumFolder(VideoAlbum album, KoofrClient koofrClient)
      throws IOException, InvalidTokenException {
    String albumName = KoofrTransmogrificationConfig.getAlbumName(album.getName());

    monitor.debug(() -> String.format("Create Koofr folder %s", albumName));

    String rootPath = koofrClient.ensureRootFolder();
    String fullPath = rootPath + "/" + albumName;

    koofrClient.ensureFolder(rootPath, albumName);

    String description = KoofrClient.trimDescription(album.getDescription());

    if (description != null && description.length() > 0) {
      koofrClient.addDescription(fullPath, description);
    }

    return fullPath;
  }

  private ItemImportResult<String> importSingleVideo(
      VideoModel video,
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      KoofrClient koofrClient)
      throws IOException, InvalidTokenException, DestinationMemoryFullException {
    monitor.debug(() -> String.format("Import single video %s", video.getName()));
    Long size = null;

    try {
      TemporaryPerJobDataStore.InputStreamWrapper inputStreamWrapper =
              connectionProvider.getInputStreamForItem(jobId, video);
      ItemImportResult<String> response;
      try (InputStream inputStream = inputStreamWrapper.getStream()) {
        String parentPath;
        if (video.getAlbumId() == null) {
          parentPath = koofrClient.ensureVideosFolder();
        } else {
          parentPath = idempotentImportExecutor.getCachedValue(video.getAlbumId());
        }

        String name = video.getName();
        String description = KoofrClient.trimDescription(video.getDescription());

        String fullPath = parentPath + "/" + name;

        if (koofrClient.fileExists(fullPath)) {
          monitor.debug(() -> String.format("Video already exists %s", video.getName()));

          return ItemImportResult.success(fullPath);
        }

        long inputStreamBytes = inputStreamWrapper.getBytes();
        response = ItemImportResult.success(koofrClient.uploadFile(
                parentPath,
                name,
                inputStream,
                video.getEncodingFormat(),
                video.getUploadedTime(),
                description), inputStreamBytes);
        size = inputStreamBytes;
      } catch (FileNotFoundException e) {
        monitor.info(
                () -> String.format("Video resource was missing for id: %s", video.getDataId()), e);
        throw e;
      }
      return response;
    } catch (FileNotFoundException e) {
      Long finalBytes = size;
      return ItemImportResult.error(e, finalBytes);
    }
  }
}
