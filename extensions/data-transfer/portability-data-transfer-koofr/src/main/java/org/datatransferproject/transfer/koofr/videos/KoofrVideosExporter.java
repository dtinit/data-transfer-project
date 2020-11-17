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

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.koofr.common.FilesFile;
import org.datatransferproject.transfer.koofr.common.FilesListRecursiveItem;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class KoofrVideosExporter
    implements Exporter<TokensAndUrlAuthData, VideosContainerResource> {

  private final Monitor monitor;

  private final KoofrClientFactory koofrClientFactory;

  public KoofrVideosExporter(KoofrClientFactory koofrClientFactory, Monitor monitor) {
    this.koofrClientFactory = koofrClientFactory;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<VideosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws CopyExceptionWithFailureReason {
    Preconditions.checkNotNull(authData);

    KoofrClient koofrClient = koofrClientFactory.create(authData);

    String rootPath = koofrClient.getRootPath();

    List<FilesListRecursiveItem> items;

    try {
      items = koofrClient.listRecursive(rootPath);
    } catch (IOException e) {
      return new ExportResult<>(e);
    }

    ArrayList<VideoAlbum> albums = new ArrayList<>();
    HashSet<String> albumsWithVideos = new HashSet<>();

    ArrayList<VideoObject> exportVideos = new ArrayList<>();

    for (FilesListRecursiveItem item : items) {
      String path = item.getPath();
      String[] pathParts = path.split("/");

      if (FilesListRecursiveItem.TYPE_FILE.equals(item.getType())) {
        FilesFile file = item.getFile();

        if (FilesFile.TYPE_DIR.equals(file.getType())) {
          if (pathParts.length == 2 && !pathParts[1].isEmpty()) {
            String albumId = path;

            String albumName = file.getName();

            if (file.getTags() != null && file.getTags().containsKey("originalName")) {
              albumName = file.getTags().get("originalName").get(0);
            }

            String description = null;

            if (file.getTags() != null && file.getTags().containsKey("description")) {
              description = file.getTags().get("description").get(0);
            }

            albums.add(new VideoAlbum(albumId, albumName, description));
          }
        } else if (pathParts.length > 2 && file.getContentType().startsWith("video/")) {
          String videoId = path;

          String videoName = file.getName();

          String description = null;

          if (file.getTags() != null && file.getTags().containsKey("description")) {
            description = file.getTags().get("description").get(0);
          }

          String contentType = file.getContentType();

          String albumId = String.join("/", Arrays.copyOfRange(pathParts, 0, 2));
          albumsWithVideos.add(albumId);

          String fullPath = rootPath + path;

          String fetchableUrl;

          try {
            fetchableUrl = koofrClient.fileLink(fullPath);
          } catch (IOException e) {
            monitor.severe(() -> String.format("Koofr file link error: %s: %s", path, e));
            continue;
          }

          exportVideos.add(
              new VideoObject(
                  videoName, fetchableUrl, description, contentType, videoId, albumId, false));
        }
      } else if (FilesListRecursiveItem.TYPE_ERROR.equals(item.getType())) {
        monitor.severe(
            () ->
                String.format(
                    "Koofr list item error: %s: %s: %s",
                    path, item.getError().getCode(), item.getError().getMessage()));
      }
    }

    ArrayList<VideoAlbum> exportAlbums = new ArrayList<>();

    for (VideoAlbum album : albums) {
      if (albumsWithVideos.contains(album.getId())) {
        exportAlbums.add(album);
      }
    }

    VideosContainerResource containerResource =
        new VideosContainerResource(exportAlbums, exportVideos);

    return new ExportResult<>(ExportResult.ResultType.END, containerResource, null);
  }
}
