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

package org.datatransferproject.datatransfer.synology.utils;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyImportException;

/**
 * Binds media items to albums, ensuring that items are only added to albums that are ready. NOTE:
 * This class is thread-safe.
 *
 * @param <K> the album key type
 */
public class SynologyMediaAlbumBinder<K> {
  private final OnAlbumReadyCallback<K> onAlbumReady;
  private final Monitor monitor;

  private final ConcurrentMap<K, K> readyAlbumMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<K, Queue<K>> pendingItemMap = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  public SynologyMediaAlbumBinder(OnAlbumReadyCallback<K> onAlbumReady, Monitor monitor) {
    this.onAlbumReady = onAlbumReady;
    this.monitor = monitor;
  }

  public void whenAlbumReady(@Nullable K albumKey, @Nullable K newAlbumKey, @Nullable UUID jobId)
      throws SynologyImportException {
    if (albumKey == null || newAlbumKey == null) {
      return;
    }
    readyAlbumMap.put(albumKey, newAlbumKey);
    monitor.debug(
        () -> "[SynologyMediaBinder] album READY",
        "albumKey:",
        albumKey,
        "newAlbumKey:",
        newAlbumKey,
        jobId);
    consumePendingItems(albumKey, jobId);
  }

  public void put(@Nullable K albumKey, @Nullable K newItemKey, @Nullable UUID jobId)
      throws SynologyImportException {
    if (albumKey == null || newItemKey == null) {
      return;
    }

    K readyAlbum = readyAlbumMap.get(albumKey);
    if (readyAlbum != null) {
      onAlbumReady.accept(readyAlbum, newItemKey, jobId);
      monitor.debug(
          () -> "[SynologyMediaBinder] item PROCESSED",
          "albumKey:",
          albumKey,
          "newItemKey:",
          newItemKey,
          jobId);
      return;
    }

    synchronized (lock) {
      // Re-check if album is ready now that we are in a synchronized block
      readyAlbum = readyAlbumMap.get(albumKey);
      if (readyAlbum != null) {
        onAlbumReady.accept(readyAlbum, newItemKey, jobId);
        monitor.debug(
            () -> "[SynologyMediaBinder] item PROCESSED (after lock)",
            "albumKey:",
            albumKey,
            "newItemKey:",
            newItemKey,
            jobId);
      } else {
        pendingItemMap
            .computeIfAbsent(albumKey, k -> new ConcurrentLinkedQueue<>())
            .add(newItemKey);
        monitor.debug(
            () -> "[SynologyMediaBinder] item PENDING",
            "albumKey:",
            albumKey,
            "newItemKey:",
            newItemKey,
            jobId);
      }
    }
  }

  private void consumePendingItems(K albumKey, UUID jobId) throws SynologyImportException {
    Queue<K> pendingNewItemKeys = pendingItemMap.remove(albumKey);
    if (pendingNewItemKeys != null) {
      K album = readyAlbumMap.get(albumKey);
      for (K newItemKey : pendingNewItemKeys) {
        onAlbumReady.accept(album, newItemKey, jobId);
      }
      monitor.debug(
          () -> "[SynologyMediaBinder] pending items CONSUMING",
          "albumKey:",
          albumKey,
          "pendingItemCount:",
          pendingNewItemKeys.size(),
          jobId);
    }
  }
}
