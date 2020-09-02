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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restfb.Connection;
import com.restfb.types.Video;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;

public class FacebookVideosExporterTest {

  private static final String VIDEO_ID = "937644721";
  private static final String VIDEO_SOURCE = "https://www.example.com/video.mp4";
  private static final String VIDEO_NAME = "Example video";

  private FacebookVideosExporter facebookVideosExporter;
  private UUID uuid = UUID.randomUUID();

  @Before
  public void setUp() throws CopyExceptionWithFailureReason {
    FacebookVideosInterface videosInterface = mock(FacebookVideosInterface.class);

    // Set up example video
    Video video = new Video();
    video.setId(VIDEO_ID);
    video.setSource(VIDEO_SOURCE);
    video.setDescription(VIDEO_NAME);

    ArrayList<Video> videos = new ArrayList<>();
    videos.add(video);

    @SuppressWarnings("unchecked")
    Connection<Video> videoConnection = mock(Connection.class);

    when(videosInterface.getVideos(Optional.empty())).thenReturn(videoConnection);
    when(videoConnection.getData()).thenReturn(videos);

    facebookVideosExporter =
        new FacebookVideosExporter(new AppCredentials("key", "secret"), videosInterface, null);
  }

  @Test
  public void testExportVideo() throws CopyExceptionWithFailureReason {
    ExportResult<VideosContainerResource> result =
        facebookVideosExporter.export(
            uuid,
            new TokensAndUrlAuthData("accessToken", null, null),
            Optional.of(new ExportInformation(null, null)));

    assertEquals(ExportResult.ResultType.END, result.getType());
    VideosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getVideos().size());
    assertEquals(
        new VideoObject(
            VIDEO_ID + ".mp4", VIDEO_SOURCE, VIDEO_NAME, "video/mp4", VIDEO_ID, null, false),
        exportedData.getVideos().toArray()[0]);
  }
}
