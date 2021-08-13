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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoModel;
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
  private ImageStreamProvider streamProvider;

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

    streamProvider = mock(ImageStreamProvider.class);
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
    long length =
        googleVideosImporter.importVideoBatch(
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false),
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    "myId2",
                    null,
                    false)),
            photosLibraryClient,
            executor);
    assertEquals("Expected the number of bytes to be the two files of 32L.", 64L, length);
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
    long length =
        googleVideosImporter.importVideoBatch(
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false),
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    "myId2",
                    null,
                    false)),
            photosLibraryClient,
            executor);

    assertEquals("Expected the number of bytes to be the one files of 32L.", 32L, length);
    assertEquals("Expected executor to have one error.", 1, executor.getErrors().size());
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals("myId2", errorDetail.id());
    assertThat(
        errorDetail.exception(), CoreMatchers.containsString("Video item could not be created."));
  }

  @Test
  public void skipNotFoundVideo() throws Exception {
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
    when(httpURLConnection.getInputStream()).thenThrow(new FileNotFoundException());
    when(streamProvider.getConnection(any())).thenReturn(httpURLConnection);

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    long length =
        googleVideosImporter.importVideoBatch(
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false)),
            photosLibraryClient,
            executor);
    assertEquals("Expected the number of bytes to be 0L.", 0L, length);
    assertEquals("Expected executor to have no errors.", 0, executor.getErrors().size());
  }

  @Test
  public void descriptionOver1kCharactersShouldNotFail() {
    // Mock creation response
    String videoDescriptionOver1k =
        "z0pyiaeQ03C7Pfs7qdoWfpR2E4BjqqLvsEL1OdBaQu8PeoI5uca83NJQKOfF4gnAhzvpgtbAPGBUade"
            + "FH5i4vas067Q67aDg1JA9qnnMEy5TTS7Qrp0MImGAI4aHFINwDrTOlFnyGoOwtQC6LLbWWlM8m224G1C08oEyjxuWicMXIdsJfsQvbE"
            + "dropW0jOMmO2DTCDCPRKwONGHPpo48pmi8HbtNolrbnU189mhyi4zSK3xmMAgmxQWOMmuNryXWB0Zok8hDxnxIes85Oe853U3jUxdu6"
            + "wNwkbZcpb97dj3puh6UYO9YFFsU40F2ULzpvPTvAPvDeBeOENpUjuh9YhPQiMbwLqne2AxLgMDgxz473Ho2DosixcWjSmX6JfSxInXh"
            + "lmXxN6xLJRi2abHeEpbOdvl28xEYpBF73DuZZ7NKPKyKhEcWi7aJVoWp9niBl0Cp4PCOO51ABROXOzE8dcoxf6dU1fhqkcQcuxV1qeK"
            + "XfYewxh8uZeShaMoey1rwzuux7lnKoHDGVQe1nJwSuTUNE5BgLa3uOSwQ9wG0tuakZF2M2YIMhEF6DUu7mZfN41fwPFleuwzO76C6eD"
            + "inP3xlNJzhsQjQtL0ITCf2oL6LgqLNxzHIRpY41d1Puxzyx2wWJ7DJy3UnMlylyEwhNkd8EuuyXYCs6GIzUXkvHRQZjN99ED6gkmnHS"
            + "SIW0QHBWOb4jHSYpK52OVMIsLkwRll8zNWci7rRXxFeMw0s0sFcIZthajvP7PMA361bNUDQe4vVhsxF1AQufm0D2SYGpA4zH8LOsacl"
            + "QPP2vKFFED90jUvbqkhesYYGvrvSq0t12LoMTFqkckRbxj7tODIUco9FFf9U5MQV40q6jgrKup19BSR9NUI58Y0GpI5ZqPgSaNhoJ5V"
            + "vsPhjrywUo6s9oOnolihQYq6lXZzwhESS8diG34oFLEwq9msSsrRtUSjgH50mNGogOlgEtbaFlMgXstzOWtUk2CwFEHZ9Y2qv123456"
            + "7890";
    final VideoModel videoModel =
        new VideoModel(
            VIDEO_TITLE, VIDEO_URI, videoDescriptionOver1k, MP4_MEDIA_TYPE, VIDEO_ID, null, false);

    String uploadToken = "token";
    NewMediaItem newMediaItemResult = googleVideosImporter.buildMediaItem(videoModel, uploadToken);
    assertFalse(
        "Expected the length of the description to be truncated to 1000 chars.",
        (newMediaItemResult.getDescription().length() > 1000));
    assertTrue(
        "Expected a truncated description to terminate with \"...\"",
        newMediaItemResult.getDescription().endsWith("..."));
  }
}
