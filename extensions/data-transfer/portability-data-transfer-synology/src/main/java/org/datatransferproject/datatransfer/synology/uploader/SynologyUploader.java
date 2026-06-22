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

package org.datatransferproject.datatransfer.synology.uploader;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.constant.SynologyConstant;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService;
import org.datatransferproject.datatransfer.synology.utils.SynologyMediaAlbumBinder;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.ImportableItem;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;

/** Handles the import of albums, photos, and videos to Synology. Business logic is handled here. */
public class SynologyUploader {
  private final IdempotentImportExecutor idempotentExecutor;
  private final Monitor monitor;
  private final SynologyDTPService synologyDTPService;

  // need a mapper to map PhotoModel.albumId to newAlbumId from SynologyDTPService.createAlbum
  private final SynologyMediaAlbumBinder<String> synologyMediaAlbumBinder;

  /**
   * Constructs a new {@code SynologyUploader} instance.
   *
   * @param idempotentExecutor the idempotent executor
   * @param monitor the monitor
   * @param synologyDTPService the Synology DTP service
   */
  public SynologyUploader(
      IdempotentImportExecutor idempotentExecutor,
      Monitor monitor,
      SynologyDTPService synologyDTPService) {
    this.idempotentExecutor = idempotentExecutor;
    this.monitor = monitor;
    this.synologyDTPService = synologyDTPService;

    this.synologyMediaAlbumBinder =
        new SynologyMediaAlbumBinder<String>(this::addItemToAlbum, monitor);
  }

  /**
   * Imports albums.
   *
   * @param albums the albums
   * @param jobId the job ID
   */
  public void importAlbums(Collection<? extends ImportableItem> albums, UUID jobId)
      throws CopyExceptionWithFailureReason {
    monitor.info(
        () -> String.format("[SynologyImporter] starts importing albums, size: %d.", albums.size()),
        jobId);
    if (albums.isEmpty()) {
      return;
    }

    for (ImportableItem album : albums) {
      try {
        MediaAlbum mediaAlbum;
        if (album instanceof MediaAlbum) {
          mediaAlbum = (MediaAlbum) album;
        } else if (album instanceof PhotoAlbum) {
          mediaAlbum = MediaAlbum.photoToMediaAlbum(((PhotoAlbum) album));
        } else if (album instanceof VideoAlbum) {
          mediaAlbum = MediaAlbum.videoToMediaAlbum(((VideoAlbum) album));
        } else {
          monitor.severe(
              () ->
                  String.format(
                      "[SynologyImporter] unsupported album type: %s, album name: %s.",
                      album.getClass(), album.getName()),
              jobId);
          continue;
        }
        String newAlbumId =
            importItemWithCache(
                mediaAlbum,
                jobId,
                "album_id",
                synologyDTPService::createAlbum,
                MediaAlbum::getId,
                MediaAlbum::getName);
        synologyMediaAlbumBinder.whenAlbumReady(mediaAlbum.getId(), newAlbumId, jobId);
      } catch (CopyExceptionWithFailureReason e) {
        monitor.severe(e::toString, jobId);
        throw e;
      } catch (Exception e) {
        monitor.severe(e::toString, jobId);
      }
    }
    monitor.info(() -> "[SynologyImporter] ended importing albums.", jobId);
  }

  /**
   * Imports photos.
   *
   * @param photos the photos
   * @param jobId the job ID
   */
  public void importPhotos(Collection<PhotoModel> photos, UUID jobId)
      throws CopyExceptionWithFailureReason {
    monitor.info(
        () -> String.format("[SynologyImporter] starts importing photos, size: %d.", photos.size()),
        jobId);
    if (photos.isEmpty()) {
      return;
    }
    monitor.info(() -> "[SynologyImporter] starts importing photos.", jobId);
    for (PhotoModel photo : photos) {
      monitor.info(
          () ->
              String.format(
                  "[SynologyImporter] starts importing photo, dataId: [%s], name: [%s]",
                  photo.getDataId(), photo.getName()),
          jobId);
      try {
        String newPhotoId =
            importItemWithCache(
                photo,
                jobId,
                "item_id",
                synologyDTPService::createPhoto,
                PhotoModel::getDataId,
                PhotoModel::getName);
        synologyMediaAlbumBinder.put(photo.getAlbumId(), newPhotoId, jobId);
      } catch (CopyExceptionWithFailureReason e) {
        monitor.severe(e::toString, jobId);
        throw e;
      } catch (Exception e) {
        monitor.severe(e::toString, jobId);
      }
    }
    monitor.info(() -> "[SynologyImporter] ended importing photos.", jobId);
  }

  /**
   * Imports videos.
   *
   * @param videos the videos
   * @param jobId the job ID
   */
  public void importVideos(Collection<VideoModel> videos, UUID jobId)
      throws CopyExceptionWithFailureReason {
    monitor.info(
        () -> String.format("[SynologyImporter] starts importing videos, size: %d", videos.size()),
        jobId);
    if (videos.isEmpty()) {
      return;
    }
    monitor.info(() -> "[SynologyImporter] starts importing videos", jobId);
    for (VideoModel video : videos) {
      monitor.info(
          () ->
              String.format(
                  "[SynologyImporter] starts importing video, dataId: [%s], name: [%s]",
                  video.getDataId(), video.getName()),
          jobId);
      try {
        String newVideoId =
            importItemWithCache(
                video,
                jobId,
                "item_id",
                synologyDTPService::createVideo,
                VideoModel::getDataId,
                VideoModel::getName);
        synologyMediaAlbumBinder.put(video.getAlbumId(), newVideoId, jobId);
      } catch (CopyExceptionWithFailureReason e) {
        monitor.severe(e::toString, jobId);
        throw e;
      } catch (Exception e) {
        monitor.severe(e::toString, jobId);
      }
    }
    monitor.info(() -> "[SynologyImporter] ended importing videos.", jobId);
  }

  @FunctionalInterface
  private interface SynologyBiFunction<T, U, R> {
    R apply(T t, U u) throws CopyExceptionWithFailureReason, IOException;
  }

  private <T> String importItemWithCache(
      T item,
      UUID jobId,
      String idField,
      SynologyBiFunction<T, UUID, Map<String, Object>> createFunction,
      Function<T, String> dataIdFunction,
      Function<T, String> nameFunction)
      throws Exception {
    return idempotentExecutor.executeAndSwallowIOExceptions(
        dataIdFunction.apply(item),
        nameFunction.apply(item),
        () -> {
          Map<String, Object> createResult = createFunction.apply(item, jobId);
          String itemId = createResult.get(idField).toString();
          monitor.info(
              () -> "[SynologyImporter] item uploaded successfully",
              jobId,
              "dataId:",
              dataIdFunction.apply(item),
              "name:",
              nameFunction.apply(item));
          return itemId;
        });
  }

  private void addItemToAlbum(String albumId, String itemId, UUID jobId)
      throws CopyExceptionWithFailureReason {
    try {
      Boolean createResult =
          idempotentExecutor.executeAndSwallowIOExceptions(
              String.format(SynologyConstant.ALBUM_ITEM_ID_FORMAT, albumId, itemId),
              itemId,
              () ->
                  (Boolean)
                      synologyDTPService.addItemToAlbum(albumId, itemId, jobId).get("success"));
      if (Boolean.FALSE.equals(createResult)) {
        monitor.severe(
            () ->
                String.format(
                    "[SynologyImporter] failed to add item to album, albumId: [%s], itemId: [%s].",
                    albumId, itemId));
      }
    } catch (CopyExceptionWithFailureReason e) {
      throw e;
    } catch (Exception e) {
      monitor.severe(
          () ->
              String.format(
                  "[SynologyImporter] failed to add item to album, albumId: [%s], itemId: [%s].",
                  albumId, itemId),
          e);
      return;
    }
    monitor.info(
        () -> "[SynologyImporter] added item to album successfully",
        jobId,
        "albumId:",
        albumId,
        "itemId:",
        itemId);
  }
}
