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

import static org.datatransferproject.datatransfer.google.videos.GoogleVideosInterface.buildPhotosLibraryClient;
import static org.datatransferproject.datatransfer.google.videos.GoogleVideosInterface.uploadBatchOfVideos;
import static org.datatransferproject.datatransfer.google.videos.GoogleVideosInterface.uploadVideo;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.UnauthenticatedException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.upload.UploadMediaItemResponse.Error;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GooglePhotosImportUtils;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  private final ConnectionProvider connectionProvider;
  private final Monitor monitor;
  private final AppCredentials appCredentials;
  private final TemporaryPerJobDataStore dataStore;
  private final Map<UUID, PhotosLibraryClient> clientsMap;

  public GoogleVideosImporter(
      AppCredentials appCredentials, TemporaryPerJobDataStore dataStore, Monitor monitor) {
    this(appCredentials, dataStore, monitor, new ConnectionProvider(dataStore), new HashMap<>());
  }

  @VisibleForTesting
  GoogleVideosImporter(
      AppCredentials appCredentials,
      TemporaryPerJobDataStore dataStore,
      Monitor monitor,
      ConnectionProvider connectionProvider,
      Map<UUID, PhotosLibraryClient> clientsMap) {
    this.connectionProvider = connectionProvider;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.dataStore = dataStore;
    this.clientsMap = clientsMap;
  }

  // TODO(aksingh737) WARNING: stop maintaining this code here; this needs to be reconciled against
  // a generic version so we don't have feature/bug development drift against our forks; see the
  // slowly-progressing effort to factor this code out with small interfaces, over in
  // GoogleMediaImporter.
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
      client = buildPhotosLibraryClient(appCredentials, authData);
      clientsMap.put(jobId, client);
    }

    for (VideoAlbum album : data.getAlbums()) {
      executor.importAndSwallowIOExceptions(album, (a) -> {
        String title = GooglePhotosImportUtils.cleanAlbumTitle(a.getName());
        return ItemImportResult.success(client.createAlbum(title).getId());
      });
    }

    long bytes = 0L;
    //     Uploads videos
    final Collection<VideoModel> videos = data.getVideos();
    if (videos != null && videos.size() > 0) {
      Stream<VideoModel> stream =
          videos.stream()
              .filter(video -> shouldImport(video, executor))
              .map(this::transformVideoName);
      // We partition into groups of 49 as 50 is the maximum number of items that can be created in
      // one call. (We use 49 to avoid potential off by one errors)
      // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
      final UnmodifiableIterator<List<VideoModel>> batches =
          Iterators.partition(stream.iterator(), 49);
      while (batches.hasNext()) {
        long batchBytes = uploadBatchOfVideos(
            jobId,
            batches.next(),
            dataStore,
            client,
            executor,
            connectionProvider,
            monitor);
        bytes += batchBytes;
      }
    }
    final ImportResult result = ImportResult.OK;
    return result.copyWithBytes(bytes);
  }

  private boolean shouldImport(DownloadableItem item, IdempotentImportExecutor executor) {
    if (item.getFetchableUrl() == null) {
      monitor.info(() -> "Content Url is empty. Make sure that you provide a valid content Url.");
      return false;
    } else {
      // If the item key is already cached there is no need to retry.
      return !executor.isKeyCached(item.getIdempotentId());
    }
  }

  private VideoModel transformVideoName(VideoModel video) {
    String filename = Strings.isNullOrEmpty(video.getName()) ? "untitled" : video.getName();
    video.setName(filename);
    return video;
  }
}
