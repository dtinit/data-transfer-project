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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.File;
import java.net.HttpURLConnection;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;

public class GoogleVideosImporterTest {

  private static final String VIDEO_TITLE = "Model video title";
  private static final String VIDEO_DESCRIPTION = "Model video description";
  private static final String VIDEO_URI = "https://www.example.com/video.mp4";
  private static final String MP4_MEDIA_TYPE = "video/mp4";
  private static final String VIDEO_ID = "myId";

  private GoogleVideosImporter googleVideosImporter;

  @Before
  public void setUp() throws Exception {
    // Create files so we can accurately check the length of file counting
    TemporaryPerJobDataStore dataStore = mock(TemporaryPerJobDataStore.class);
    TemporaryFolder folder = new TemporaryFolder();
    folder.create();
    File file1 = folder.newFile();
    Files.write(new byte[32], file1);
    File file2 = folder.newFile();
    Files.write(new byte[32], file2);
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(file1, file2);

    ImageStreamProvider streamProvider = mock(ImageStreamProvider.class);
    when(streamProvider.getConnection(any())).thenReturn(mock(HttpURLConnection.class));
    googleVideosImporter =
        new GoogleVideosImporter(null, dataStore, mock(Monitor.class), streamProvider);
  }

  @Test
  public void importTwoVideos() throws Exception {
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    // Mock uploads
    when(photosLibraryClient.uploadMediaItem(any()))
        .thenReturn(
            UploadMediaItemResponse.newBuilder().setUploadToken("token1").build(),
            UploadMediaItemResponse.newBuilder().setUploadToken("token2").build());

    // Mock creation response
    final NewMediaItemResult newMediaItemResult =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setMediaItem(MediaItem.newBuilder().setId("RESULT_ID_1").build())
            .setUploadToken("token1")
            .build();
    final NewMediaItemResult newMediaItemResult2 =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setMediaItem(MediaItem.newBuilder().setId("RESULT_ID_2").build())
            .setUploadToken("token2")
            .build();
    BatchCreateMediaItemsResponse response =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult)
            .addNewMediaItemResults(newMediaItemResult2)
            .build();
    when(photosLibraryClient.batchCreateMediaItems(ArgumentMatchers.anyList()))
        .thenReturn(response);

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    Long length =
        googleVideosImporter.importVideoBatch(
            Lists.newArrayList(
                new VideoObject(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false),
                new VideoObject(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    "myId2",
                    null,
                    false)),
            photosLibraryClient,
            executor);
    assertEquals(
        "Expected the number of bytes to be the two files of 32L.", 64L, length.longValue());
    assertEquals("Expected executor to have no errors.", 0, executor.getErrors().size());
  }

  @Test
  public void failOneVideo() throws Exception {
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    // Mock uploads
    when(photosLibraryClient.uploadMediaItem(any()))
        .thenReturn(
            UploadMediaItemResponse.newBuilder().setUploadToken("token1").build(),
            UploadMediaItemResponse.newBuilder().setUploadToken("token2").build());

    // Mock creation response
    final NewMediaItemResult newMediaItemResult =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setMediaItem(MediaItem.newBuilder().setId("RESULT_ID_1").build())
            .setUploadToken("token1")
            .build();
    final NewMediaItemResult newMediaItemResult2 =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).build())
            .setUploadToken("token2")
            .build();
    BatchCreateMediaItemsResponse response =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult)
            .addNewMediaItemResults(newMediaItemResult2)
            .build();
    when(photosLibraryClient.batchCreateMediaItems(ArgumentMatchers.anyList()))
        .thenReturn(response);

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    Long length =
        googleVideosImporter.importVideoBatch(
            Lists.newArrayList(
                new VideoObject(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false),
                new VideoObject(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    "myId2",
                    null,
                    false)),
            photosLibraryClient,
            executor);

    assertEquals(
        "Expected the number of bytes to be the one files of 32L.", 32L, length.longValue());
    assertEquals("Expected executor to have one error.", 1, executor.getErrors().size());
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals("myId2", errorDetail.id());
    assertThat(
        errorDetail.exception(), CoreMatchers.containsString("Video item could not be created."));
  }
}
