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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.google.common.collect.Lists;
import com.google.rpc.Code;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.datatransfer.google.mediaModels.Status;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.hamcrest.CoreMatchers;
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
  private UUID uuid = UUID.randomUUID();
  private GooglePhotosImporter googlePhotosImporter;
  private GooglePhotosInterface googlePhotosInterface;
  private IdempotentImportExecutor executor;

  @Before
  public void setUp() throws IOException, InvalidTokenException, PermissionDeniedException {
    googlePhotosInterface = Mockito.mock(GooglePhotosInterface.class);
    Monitor monitor = Mockito.mock(Monitor.class);
    executor = new InMemoryIdempotentImportExecutor(monitor);

    Mockito.when(
            googlePhotosInterface.makePostRequest(
                anyString(), any(), any(), eq(NewMediaItemResult.class)))
        .thenReturn(Mockito.mock(NewMediaItemResult.class));

    TemporaryPerJobDataStore jobStore = new LocalJobStore();

    InputStream inputStream = Mockito.mock(InputStream.class);
    ImageStreamProvider imageStreamProvider = Mockito.mock(ImageStreamProvider.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Mockito.when(imageStreamProvider.getConnection(anyString())).thenReturn(conn);
    Mockito.when(conn.getInputStream()).thenReturn(inputStream);
    Mockito.when(conn.getContentLengthLong()).thenReturn(32L);

    googlePhotosImporter =
        new GooglePhotosImporter(
            null, jobStore, null, null, googlePhotosInterface, imageStreamProvider, monitor, 1.0);
  }

  @Test
  public void importAlbum() throws Exception {
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
  public void importTwoPhotos() throws Exception {
    PhotoModel photoModel1 =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            false);
    PhotoModel photoModel2 =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID2",
            OLD_ALBUM_ID,
            false);

    Mockito.when(googlePhotosInterface.uploadPhotoContent(any())).thenReturn("token1", "token2");
    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[] {
              buildMediaItemResult("token1", Code.OK_VALUE),
              buildMediaItemResult("token2", Code.OK_VALUE)
            });
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    long length =
        googlePhotosImporter.importPhotoBatch(
            UUID.randomUUID(),
            Mockito.mock(TokensAndUrlAuthData.class),
            Lists.newArrayList(photoModel1, photoModel2),
            executor,
            NEW_ALBUM_ID);
    // Two photos of 32L each imported
    assertEquals(64L, length);
    assertTrue(executor.isKeyCached(googlePhotosImporter.getIdempotentId(photoModel1)));
    assertTrue(executor.isKeyCached(googlePhotosImporter.getIdempotentId(photoModel2)));
  }

  private NewMediaItemResult buildMediaItemResult(String uploadToken, int code) {
    // We do a lot of mocking as building the actual objects would require changing the constructors
    // which messed up deserialization so best to leave them unchanged.
    GoogleMediaItem mediaItem = Mockito.mock(GoogleMediaItem.class);
    Mockito.when(mediaItem.getId()).thenReturn("newId");
    Status status = Mockito.mock(Status.class);
    Mockito.when(status.getCode()).thenReturn(code);
    NewMediaItemResult result = Mockito.mock(NewMediaItemResult.class);
    Mockito.when(result.getUploadToken()).thenReturn(uploadToken);
    Mockito.when(result.getStatus()).thenReturn(status);
    Mockito.when(result.getMediaItem()).thenReturn(mediaItem);
    return result;
  }

  @Test
  public void importTwoPhotosWithFailure() throws Exception {
    PhotoModel photoModel1 =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            false);
    PhotoModel photoModel2 =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID2",
            OLD_ALBUM_ID,
            false);

    Mockito.when(googlePhotosInterface.uploadPhotoContent(any())).thenReturn("token1", "token2");
    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[] {
              buildMediaItemResult("token1", Code.OK_VALUE),
              buildMediaItemResult("token2", Code.UNAUTHENTICATED_VALUE)
            });
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    long length =
        googlePhotosImporter.importPhotoBatch(
            UUID.randomUUID(),
            Mockito.mock(TokensAndUrlAuthData.class),
            Lists.newArrayList(photoModel1, photoModel2),
            executor,
            NEW_ALBUM_ID);
    // Only one photo of 32L imported
    assertEquals(32L, length);
    assertTrue(executor.isKeyCached(googlePhotosImporter.getIdempotentId(photoModel1)));
    String failedDataId = googlePhotosImporter.getIdempotentId(photoModel2);
    assertFalse(executor.isKeyCached(failedDataId));
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals(failedDataId, errorDetail.id());
    assertThat(errorDetail.exception(), CoreMatchers.containsString("Photo could not be created."));
  }
}
