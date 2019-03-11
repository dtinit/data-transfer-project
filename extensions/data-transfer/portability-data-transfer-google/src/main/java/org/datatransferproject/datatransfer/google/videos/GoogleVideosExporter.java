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

package org.datatransferproject.datatransfer.google.videos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GoogleVideosExporter
        implements Exporter<TokensAndUrlAuthData, VideosContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private volatile GoogleVideosInterface videosInterface;
  private JsonFactory jsonFactory;

  public GoogleVideosExporter(GoogleCredentialFactory credentialFactory, JsonFactory jsonFactory) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
  }

  @VisibleForTesting
  GoogleVideosExporter(
          GoogleCredentialFactory credentialFactory, GoogleVideosInterface videosInterface) {
    this.credentialFactory = credentialFactory;
    this.videosInterface = videosInterface;
  }

  @Override
  public ExportResult<VideosContainerResource> export(
          UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
          throws IOException {

    return exportVideos(
            authData, exportInformation.map(e -> (StringPaginationToken) e.getPaginationData()));
  }

  @VisibleForTesting
  ExportResult<VideosContainerResource> exportVideos(
          TokensAndUrlAuthData authData, Optional<StringPaginationToken> paginationData)
          throws IOException {

    Optional<String> paginationToken = paginationData.map(StringPaginationToken::getToken);

    MediaItemSearchResponse mediaItemSearchResponse =
            getOrCreateVideosInterface(authData).listVideoItems(paginationToken);

    PaginationData nextPageData = null;
    if (!Strings.isNullOrEmpty(mediaItemSearchResponse.getNextPageToken())) {
      nextPageData = new StringPaginationToken(mediaItemSearchResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    VideosContainerResource containerResource = null;
    GoogleMediaItem[] mediaItems = mediaItemSearchResponse.getMediaItems();
    if (mediaItems != null && mediaItems.length > 0) {
      List<VideoObject> videos = convertVideosList(mediaItems);
      containerResource = new VideosContainerResource(null, videos);
    }

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  private List<VideoObject> convertVideosList(GoogleMediaItem[] mediaItems) {
    List<VideoObject> videos = new ArrayList<>(mediaItems.length);

    for (GoogleMediaItem mediaItem : mediaItems) {
      if (mediaItem.getMediaMetadata().getVideo() != null) {

        videos.add(convertToVideoObject(mediaItem));
      }
    }
    return videos;
  }

  private VideoObject convertToVideoObject(GoogleMediaItem mediaItem) {
    Preconditions.checkArgument(mediaItem.getMediaMetadata().getVideo() != null);

    return new VideoObject(
            "", // TODO: no title?
            //            dv = download video otherwise you only get a thumbnail
            mediaItem.getBaseUrl() + "=dv",
            mediaItem.getDescription(),
            mediaItem.getMimeType(),
            mediaItem.getId(),
            null,
            false);
  }

  private synchronized GoogleVideosInterface getOrCreateVideosInterface(
          TokensAndUrlAuthData authData) {
    return videosInterface == null ? makeVideosInterface(authData) : videosInterface;
  }

  private synchronized GoogleVideosInterface makeVideosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GoogleVideosInterface(credential, this.jsonFactory);
  }
}
