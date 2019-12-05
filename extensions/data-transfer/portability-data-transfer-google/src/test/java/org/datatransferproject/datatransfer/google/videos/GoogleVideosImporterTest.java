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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class GoogleVideosImporterTest {

  private static final String VIDEO_TITLE = "Model video title";
  private static final String VIDEO_DESCRIPTION = "Model video description";
  private static final String VIDEO_URI = "https://www.example.com/video.mp4";
  private static final String MP4_MEDIA_TYPE = "video/mp4";
  private static final String UPLOAD_TOKEN = "uploadToken";
  private static final String VIDEO_ID = "myId";
  private static final String RESULT_ID = "RESULT_ID";

  private GoogleVideosImporter googleVideosImporter;
  private PhotosLibraryClient photosLibraryClient;

  @Before
  public void setUp() throws Exception {
    googleVideosImporter = new GoogleVideosImporter(null, null, null);

    photosLibraryClient = mock(PhotosLibraryClient.class);

    final NewMediaItemResult newMediaItemResult =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setMediaItem(MediaItem.newBuilder().setId(RESULT_ID).build())
            .build();
    BatchCreateMediaItemsResponse response =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult)
            .build();
    when(photosLibraryClient.batchCreateMediaItems(ArgumentMatchers.anyList()))
        .thenReturn(response);
  }

  @Test
  public void exportVideo() throws Exception {
    // Set up
    VideoObject videoModel =
        new VideoObject(
            VIDEO_TITLE, VIDEO_URI, VIDEO_DESCRIPTION, MP4_MEDIA_TYPE, VIDEO_ID, null, false);

    // Run test
    final String resultId =
        googleVideosImporter.createMediaItem(videoModel, photosLibraryClient, UPLOAD_TOKEN);

    assertEquals(RESULT_ID, resultId);
  }
}
