/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.google.photos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GooglePhotosImporterTest {

  private static final String OLD_ALBUM_ID = "OLD_ALBUM_ID";
  private static final String NEW_ALBUM_ID = "NEW_ALBUM_ID";
  private String PHOTO_TITLE = "Model photo title";
  private String PHOTO_DESCRIPTION = "Model photo description";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";
  private String UPLOAD_TOKEN = "uploadToken";
  private UUID uuid = UUID.randomUUID();
  private GooglePhotosImporter googlePhotosImporter;
  private GooglePhotosInterface googlePhotosInterface;
  private TemporaryPerJobDataStore jobStore;
  private ImageStreamProvider imageStreamProvider;
  private InputStream inputStream;
  private IdempotentImportExecutor executor;
  private Monitor monitor;

  @Before
  public void setUp() throws IOException, InvalidTokenException, PermissionDeniedException {
    executor = new FakeIdempotentImportExecutor();
    googlePhotosInterface = Mockito.mock(GooglePhotosInterface.class);
    monitor = Mockito.mock(Monitor.class);

    Mockito.when(googlePhotosInterface.uploadPhotoContent(any(InputStream.class)))
        .thenReturn(UPLOAD_TOKEN);
    Mockito.when(
            googlePhotosInterface.makePostRequest(
                anyString(), any(), any(), eq(NewMediaItemResult.class)))
        .thenReturn(Mockito.mock(NewMediaItemResult.class));

    jobStore = new LocalJobStore();

    inputStream = Mockito.mock(InputStream.class);
    imageStreamProvider = Mockito.mock(ImageStreamProvider.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Mockito.when(imageStreamProvider.getConnection(anyString())).thenReturn(conn);
    Mockito.when(conn.getInputStream()).thenReturn(inputStream);

    googlePhotosImporter =
        new GooglePhotosImporter(
            null, jobStore, null, null, googlePhotosInterface, imageStreamProvider, monitor, 1.0);
  }

  @Test
  public void exportAlbum() throws Exception {
    // Set up
    String albumName = "Album Name";
    String albumDescription = "Album description";
    PhotoAlbum albumModel = new PhotoAlbum(OLD_ALBUM_ID, albumName, albumDescription);

    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setId(NEW_ALBUM_ID);
    Mockito.when(googlePhotosInterface.createAlbum(any(GoogleAlbum.class)))
        .thenReturn(responseAlbum);

    // Run test
    googlePhotosImporter.importSingleAlbum(uuid, null, albumModel);

    // Check results
    ArgumentCaptor<GoogleAlbum> albumArgumentCaptor = ArgumentCaptor.forClass(GoogleAlbum.class);
    Mockito.verify(googlePhotosInterface).createAlbum(albumArgumentCaptor.capture());
    assertEquals(albumArgumentCaptor.getValue().getTitle(), "Copy of " + albumName);
    assertNull(albumArgumentCaptor.getValue().getId());
  }

  @Test
  public void exportPhoto() throws Exception {
    // Set up
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID",
            OLD_ALBUM_ID,
            false);

    executor.executeOrThrowException(OLD_ALBUM_ID, OLD_ALBUM_ID, () -> NEW_ALBUM_ID);

    NewMediaItemResult newMediaItemResult = Mockito.mock(NewMediaItemResult.class);
    GoogleMediaItem googleMediaItem = new GoogleMediaItem();
    googleMediaItem.setId("NewId");
    Mockito.when(newMediaItemResult.getMediaItem()).thenReturn(googleMediaItem);

    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(new NewMediaItemResult[] {newMediaItemResult});
    Mockito.when(googlePhotosInterface.createPhoto(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    // Run test
    googlePhotosImporter.importSinglePhoto(uuid, null, photoModel, executor);

    // Check results
    Mockito.verify(imageStreamProvider).getConnection(IMG_URI);
    Mockito.verify(googlePhotosInterface).uploadPhotoContent(inputStream);

    ArgumentCaptor<NewMediaItemUpload> uploadArgumentCaptor =
        ArgumentCaptor.forClass(NewMediaItemUpload.class);
    Mockito.verify(googlePhotosInterface).createPhoto(uploadArgumentCaptor.capture());
    assertEquals(uploadArgumentCaptor.getValue().getAlbumId(), NEW_ALBUM_ID);
    List<NewMediaItem> newMediaItems = uploadArgumentCaptor.getValue().getNewMediaItems();
    assertEquals(newMediaItems.size(), 1);
    NewMediaItem mediaItem = newMediaItems.get(0);
    assertEquals(mediaItem.getSimpleMediaItem().getUploadToken(), UPLOAD_TOKEN);
    assertEquals(mediaItem.getDescription(), "Copy of " + PHOTO_DESCRIPTION);
  }
}
