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

package org.datatransferproject.transfer.facebook.videos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.exception.FacebookGraphException;
import com.restfb.types.Video;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class FacebookVideosExporter
    implements Exporter<TokensAndUrlAuthData, VideosContainerResource> {

  private final Monitor monitor;
  private AppCredentials appCredentials;
  private FacebookVideosInterface videosInterface;

  public FacebookVideosExporter(AppCredentials appCredentials, Monitor monitor) {
    this.appCredentials = appCredentials;
    this.monitor = monitor;
  }

  @VisibleForTesting
  FacebookVideosExporter(
      AppCredentials appCredentials, FacebookVideosInterface videosInterface, Monitor monitor) {
    this.appCredentials = appCredentials;
    this.videosInterface = videosInterface;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<VideosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws CopyExceptionWithFailureReason {
    Preconditions.checkNotNull(authData);

    return exportVideos(
        authData, exportInformation.map(e -> (StringPaginationToken) e.getPaginationData()));
  }

  private ExportResult<VideosContainerResource> exportVideos(
      TokensAndUrlAuthData authData, Optional<StringPaginationToken> paginationData)
      throws CopyExceptionWithFailureReason {

    Optional<String> paginationToken = paginationData.map(StringPaginationToken::getToken);

    try {
      Connection<Video> videoConnection =
          getOrCreateVideosInterface(authData).getVideos(paginationToken);
      List<Video> videos = videoConnection.getData();

      if (videos.isEmpty()) {
        return new ExportResult<>(ExportResult.ResultType.END, null);
      }

      ArrayList<VideoObject> exportVideos = new ArrayList<>();
      for (Video video : videos) {
        final String url = video.getSource();
        final String fbid = video.getId();
        if (null == url || url.isEmpty()) {
          monitor.severe(() -> String.format("Source was missing or empty for video %s", fbid));
          continue;
        }
        exportVideos.add(
            new VideoObject(
                String.format("%s.mp4", fbid),
                url,
                video.getDescription(),
                "video/mp4",
                fbid,
                null,
                false));
      }

      String token = videoConnection.getAfterCursor();
      if (Strings.isNullOrEmpty(token)) {
        return new ExportResult<>(
            ExportResult.ResultType.END, new VideosContainerResource(null, exportVideos));
      } else {
        PaginationData nextPageData = new StringPaginationToken(token);
        ContinuationData continuationData = new ContinuationData(nextPageData);
        return new ExportResult<>(
            ExportResult.ResultType.CONTINUE,
            new VideosContainerResource(null, exportVideos),
            continuationData);
      }
    } catch (FacebookGraphException e) {
      String message = e.getMessage();
      // This error means the object we are trying to copy does not exist any more.
      // In such case, we should skip this object and continue with the rest of the transfer.
      if (message != null && message.contains("code 100, subcode 33")) {
        monitor.info(() -> "Cannot find videos to export, skipping to the next bunch", e);
        return new ExportResult<>(ExportResult.ResultType.END, null);
      }
      throw e;
    }
  }

  private synchronized FacebookVideosInterface getOrCreateVideosInterface(
      TokensAndUrlAuthData authData) {
    return videosInterface == null ? makeVideosInterface(authData) : videosInterface;
  }

  private synchronized FacebookVideosInterface makeVideosInterface(TokensAndUrlAuthData authData) {
    videosInterface = new RestFbFacebookVideos(authData, appCredentials);
    return videosInterface;
  }
}
