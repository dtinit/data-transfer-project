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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.MediaMetadata;
import org.datatransferproject.datatransfer.google.mediaModels.Video;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleVideosExporterTest {

  private String VIDEO_URI = "videouri";
  private String VIDEO_ID = "video id";
  private String VIDEO_TOKEN = "video_token";

  private UUID uuid = UUID.randomUUID();

  private GoogleVideosExporter googleVideosExporter;
  private TemporaryPerJobDataStore jobStore;
  private GoogleVideosInterface videosInterface;

  private MediaItemSearchResponse mediaItemSearchResponse;
  private AlbumListResponse albumListResponse;

  @BeforeEach
  public void setup() throws IOException {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    jobStore = mock(TemporaryPerJobDataStore.class);
    videosInterface = mock(GoogleVideosInterface.class);

    albumListResponse = mock(AlbumListResponse.class);
    mediaItemSearchResponse = mock(MediaItemSearchResponse.class);

    googleVideosExporter = new GoogleVideosExporter(credentialFactory, videosInterface);

    when(videosInterface.listVideoItems(any(Optional.class)))
            .thenReturn(mediaItemSearchResponse);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportSingleVideo() throws IOException {
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSingleVideo(VIDEO_URI, VIDEO_ID);
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(VIDEO_TOKEN);

    // Run test
    ExportResult<VideosContainerResource> result =
            googleVideosExporter.exportVideos(null, Optional.empty());


    // Verify correct methods were called
    verify(videosInterface).listVideoItems(Optional.empty());
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
            (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(VIDEO_TOKEN);

    // Check videos field of container
    Collection<VideoModel> actualVideos = result.getExportedData().getVideos();

    URI video_uri_object = null;
    try {
      video_uri_object = new URI(VIDEO_URI + "=dv");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    assertThat(actualVideos.stream().map(VideoModel::getContentUrl).collect(Collectors.toList()))
            .containsExactly(video_uri_object);
    // Since albums are not supported atm, this should be null
    assertThat(actualVideos.stream().map(VideoModel::getAlbumId).collect(Collectors.toList()))
            .containsExactly((Object) null);
  }

  /**
   * Sets up a response for a single video
   */
  private GoogleMediaItem setUpSingleVideo(String videoUri, String videoId) {
    GoogleMediaItem videoEntry = new GoogleMediaItem();
    videoEntry.setDescription("Description");
    videoEntry.setMimeType("video/mp4");
    videoEntry.setBaseUrl(videoUri);
    videoEntry.setId(videoId);
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setVideo(new Video());
    videoEntry.setMediaMetadata(mediaMetadata);

    return videoEntry;
  }
}
