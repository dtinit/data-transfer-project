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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;

public class GoogleVideosImporterTest {

  private static final String VIDEO_TITLE = "Model video title";
  private static final String VIDEO_DESCRIPTION = "Model video description";
  private static final String VIDEO_URI = "https://www.example.com/video.mp4";
  private static final String MP4_MEDIA_TYPE = "video/mp4";
  private static final String VIDEO_ID = "myId";
  private static final String ALBUM_ID = "album1";

  private GoogleVideosImporter googleVideosImporter;
  private TemporaryPerJobDataStore dataStore;
  private ConnectionProvider connectionProvider;
  private PhotosLibraryClient client;
  private UUID jobId;


  @BeforeEach
  public void setUp() throws Exception {
    // Create files so we can accurately check the length of file counting
    dataStore = mock(TemporaryPerJobDataStore.class);
    TemporaryFolder folder = new TemporaryFolder();
    folder.create();
    File file1 = folder.newFile();
    Files.write(new byte[32], file1);
    File file2 = folder.newFile();
    Files.write(new byte[32], file2);
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(file1, file2);

    when(dataStore.getStream(any(), anyString())).thenReturn(
        new TemporaryPerJobDataStore.InputStreamWrapper(
            new ByteArrayInputStream("TestingBytes".getBytes())));
    doNothing().when(dataStore).removeData(any(), anyString());

    connectionProvider = mock(ConnectionProvider.class);
    when(connectionProvider.getInputStreamForItem(any(), any()))
        .thenReturn(mock(InputStreamWrapper.class));
    client = mock(PhotosLibraryClient.class);
    jobId = UUID.randomUUID();
    googleVideosImporter =
        new GoogleVideosImporter(
            null, dataStore, mock(Monitor.class), connectionProvider, Map.of(jobId, client));
  }

  @Test
  public void importTwoVideosInDifferentAlbums() throws Exception {
    String googleAlbumId = "googleId";
    Album expected = Album.newBuilder().setId(googleAlbumId).setTitle("albumName").build();
    when(client.createAlbum(anyString())).thenReturn(expected);

    // Mock uploads
    when(client.uploadMediaItem(any()))
        .thenReturn(
            UploadMediaItemResponse.newBuilder().setUploadToken("token1").build(),
            UploadMediaItemResponse.newBuilder().setUploadToken("token2").build());

    // Mock creation response
    final NewMediaItemResult newMediaItemResult =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setUploadToken("token1")
            .build();
    final NewMediaItemResult newMediaItemResult2 =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setUploadToken("token2")
            .build();
    BatchCreateMediaItemsResponse response =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult)
            .build();
    BatchCreateMediaItemsResponse response2 =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult2)
            .build();
    NewMediaItem mediaItem = NewMediaItemFactory.createNewMediaItem("token1", VIDEO_DESCRIPTION);
    NewMediaItem mediaItem2 = NewMediaItemFactory.createNewMediaItem("token2", VIDEO_DESCRIPTION);
    when(client.batchCreateMediaItems(eq(googleAlbumId), eq(List.of(mediaItem))))
        .thenReturn(response);
    when(client.batchCreateMediaItems(eq(List.of(mediaItem2))))
        .thenReturn(response2);

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    long length =
        googleVideosImporter
            .importItem(
                jobId,
                executor,
                mock(TokensAndUrlAuthData.class),
                new VideosContainerResource(
                    List.of(new VideoAlbum(ALBUM_ID, "name", null)),
                    List.of(
                        new VideoModel(
                            VIDEO_TITLE,
                            VIDEO_URI,
                            VIDEO_DESCRIPTION,
                            MP4_MEDIA_TYPE,
                            VIDEO_ID,
                            ALBUM_ID,
                            false,
                            null),
                        new VideoModel(
                            VIDEO_TITLE,
                            VIDEO_URI,
                            VIDEO_DESCRIPTION,
                            MP4_MEDIA_TYPE,
                            "myId2",
                            null,
                            false,
                            null))))
            .getBytes()
            .get();
    assertEquals(64L, length,"Expected the number of bytes to be the two files of 32L.");
    assertEquals(0, executor.getErrors().size(),"Expected executor to have no errors.");
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
    UUID jobId = UUID.randomUUID();

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    long length =
        googleVideosImporter.importVideoBatch(jobId,
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false,
                    null),
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    "myId2",
                    null,
                    false,
                    null)),
            photosLibraryClient,
            executor);

    assertEquals(32L, length,"Expected the number of bytes to be the one files of 32L.");
    assertEquals(1, executor.getErrors().size(),"Expected executor to have one error.");
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals("myId2", errorDetail.id());
    assertThat(errorDetail.exception()).contains("Video item could not be created.");
  }

  @Test
  public void skipNotFoundVideo() throws Exception {
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    when(connectionProvider.getInputStreamForItem(any(), any()))
        .thenThrow(new FileNotFoundException());
    UUID jobId = UUID.randomUUID();

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    long length =
        googleVideosImporter.importVideoBatch(
            jobId,
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    false,
                    null)),
            photosLibraryClient,
            executor);
    assertEquals(0L, length,"Expected the number of bytes to be 0L.");
    assertEquals(0, executor.getErrors().size(),"Expected executor to have no errors.");
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
            VIDEO_TITLE, VIDEO_URI, videoDescriptionOver1k, MP4_MEDIA_TYPE, VIDEO_ID, null, false, null);

    String uploadToken = "token";
    NewMediaItem newMediaItemResult = googleVideosImporter.buildMediaItem(videoModel, uploadToken);
    assertFalse(
        (newMediaItemResult.getDescription().length() > 1000),"Expected the length of the description to be truncated to 1000 chars.");
    assertTrue(
        newMediaItemResult.getDescription().endsWith("..."),"Expected a truncated description to terminate with \"...\"");
  }

  @Test
  public void importVideoInTempStore() throws Exception {
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    // Mock uploads
    when(photosLibraryClient.uploadMediaItem(any()))
        .thenReturn(UploadMediaItemResponse.newBuilder().setUploadToken("token1").build());

    // Mock creation response
    final NewMediaItemResult newMediaItemResult =
        NewMediaItemResult.newBuilder()
            .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
            .setMediaItem(MediaItem.newBuilder().setId("RESULT_ID_1").build())
            .setUploadToken("token1")
            .build();
    BatchCreateMediaItemsResponse response =
        BatchCreateMediaItemsResponse.newBuilder()
            .addNewMediaItemResults(newMediaItemResult)
            .build();
    when(photosLibraryClient.batchCreateMediaItems(ArgumentMatchers.anyList()))
        .thenReturn(response);
    UUID jobId = UUID.randomUUID();

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    long length =
        googleVideosImporter.importVideoBatch(jobId,
            Lists.newArrayList(
                new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_URI,
                    VIDEO_DESCRIPTION,
                    MP4_MEDIA_TYPE,
                    VIDEO_ID,
                    null,
                    true,
                    null)),
            photosLibraryClient,
            executor);
    assertThat(length).isEqualTo(32);
    assertThat(executor.getErrors()).isEmpty();
    verify(dataStore).removeData(any(), eq(VIDEO_URI));
  }

  @Test
  public void importVideoInTempStoreFailure() throws Exception {
    when(dataStore.getStream(any(), anyString())).thenThrow(new IOException("Unit Testing"));

    UUID jobId = UUID.randomUUID();

    InMemoryIdempotentImportExecutor executor =
        new InMemoryIdempotentImportExecutor(mock(Monitor.class));
    ConnectionProvider connectionProvider = new ConnectionProvider(dataStore);
    GoogleVideosImporter googleVideosImporter =
        new GoogleVideosImporter(
            null, dataStore, mock(Monitor.class), connectionProvider, Map.of(jobId, client));
    googleVideosImporter.importVideoBatch(jobId,
        Lists.newArrayList(
            new VideoModel(
                VIDEO_TITLE,
                VIDEO_URI,
                VIDEO_DESCRIPTION,
                MP4_MEDIA_TYPE,
                VIDEO_ID,
                null,
                true,
                null)),
        mock(PhotosLibraryClient.class),
        executor);
    // should only remove the video from temp store upon success
    verify(dataStore, never()).removeData(any(), anyString());
    verify(dataStore).getStream(any(), eq(VIDEO_URI));
  }
}
