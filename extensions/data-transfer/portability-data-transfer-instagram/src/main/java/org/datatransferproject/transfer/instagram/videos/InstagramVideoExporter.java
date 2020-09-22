/*
 * Copyright 2020 The Data Transfer Project Authors.
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
package org.datatransferproject.transfer.instagram.videos;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.instagram.common.InstagramApiClient;
import org.datatransferproject.transfer.instagram.model.Child;
import org.datatransferproject.transfer.instagram.model.MediaFeedData;
import org.datatransferproject.transfer.instagram.model.MediaResponse;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class InstagramVideoExporter
    implements Exporter<TokensAndUrlAuthData, VideosContainerResource> {

  private static final String DEFAULT_ALBUM_ID = "Instagram Videos";

  private final HttpTransport httpTransport;
  private final Monitor monitor;
  private final AppCredentials appCredentials;
  private InstagramApiClient instagramApiClient;

  public InstagramVideoExporter(
      HttpTransport httpTransport, Monitor monitor, AppCredentials appCredentials) {
    this.httpTransport = httpTransport;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
  }

  @Override
  public ExportResult<VideosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {
    if (this.instagramApiClient == null) {
      this.instagramApiClient =
          new InstagramApiClient(this.httpTransport, this.monitor, this.appCredentials, authData);
    }

    if (exportInformation.isPresent()) {
      return exportVideos(exportInformation.get().getPaginationData());
    } else {
      return exportAlbum();
    }
  }

  private ExportResult<VideosContainerResource> exportAlbum() {
    List<VideoAlbum> defaultAlbums =
        Arrays.asList(
            new VideoAlbum(
                DEFAULT_ALBUM_ID, "Imported Instagram Videos", "Videos imported from instagram"));
    return new ExportResult<>(
        ResultType.CONTINUE,
        new VideosContainerResource(defaultAlbums, null),
        new ContinuationData(new StringPaginationToken(InstagramApiClient.getMediaBaseUrl())));
  }

  private VideoObject createVideoModel(String id, String mediaUrl, String caption) {
    return new VideoObject(
        String.format("%s.mp4", id), mediaUrl, caption, "video/mp4", id, DEFAULT_ALBUM_ID, false);
  }

  private void addVideosInCarouselAlbum(ArrayList<VideoObject> videos, MediaFeedData data) {
    for (Child video : data.getChildren().getData()) {
      if (video.getMediaType().equals("VIDEO")) {
        videos.add(createVideoModel(video.getId(), video.getMediaUrl(), data.getCaption()));
      }
    }
  }

  private ExportResult<VideosContainerResource> exportVideos(PaginationData pageData)
      throws IOException {
    Preconditions.checkNotNull(pageData);
    MediaResponse response;
    try {
      StringPaginationToken paginationToken = (StringPaginationToken) pageData;
      String url = paginationToken.getToken();
      response = instagramApiClient.makeRequest(url, MediaResponse.class);
    } catch (IOException e) {
      monitor.info(() -> "Failed to get videos from instagram API", e);
      throw e;
    }

    ArrayList<VideoObject> videos = new ArrayList<>();

    for (MediaFeedData data : response.getData()) {
      if (data.getMediaType().equals("CAROUSEL_ALBUM")) {
        addVideosInCarouselAlbum(videos, data);
        continue;
      }

      if (data.getMediaType().equals("VIDEO")) {
        videos.add(createVideoModel(data.getId(), data.getMediaUrl(), data.getCaption()));
        continue;
      }
    }

    String url = InstagramApiClient.getContinuationUrl(response);
    if (url == null) {
      return new ExportResult<>(ResultType.END, new VideosContainerResource(null, videos));
    }

    return new ExportResult<>(
        ResultType.CONTINUE,
        new VideosContainerResource(null, videos),
        new ContinuationData(new StringPaginationToken(url)));
  }
}
