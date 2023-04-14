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
package org.datatransferproject.datatransfer.google.media;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.rpc.Code;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import com.google.photos.library.v1.PhotosLibraryClient;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.datatransfer.google.mediaModels.Status;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GoogleMediaImporterTest {

  private static final String OLD_ALBUM_ID = "OLD_ALBUM_ID";
  private static final String NEW_ALBUM_ID = "NEW_ALBUM_ID";
  private String PHOTO_TITLE = "Model photo title";
  private String PHOTO_DESCRIPTION = "Model photo description";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";
  private String SHA1 = "11aa11AAff11aa11AAFF11aa11AAff11aa11AAFF";
  private UUID uuid = UUID.randomUUID();
  private GoogleMediaImporter googlePhotosImporter;
  private GooglePhotosInterface googlePhotosInterface;
  private PhotosLibraryClient photosLibraryClient;
  private IdempotentImportExecutor executor;
  private ConnectionProvider connectionProvider;
  private Monitor monitor;

  @Before
  public void setUp() throws Exception {
    googlePhotosInterface = mock(GooglePhotosInterface.class);
    monitor = mock(Monitor.class);

    // Initialize the executor with an old album ID -> new album ID mapping.
    executor = new InMemoryIdempotentImportExecutor(monitor);
    executor.executeOrThrowException(OLD_ALBUM_ID, "unused_item_name", () -> NEW_ALBUM_ID);

    Mockito.when(googlePhotosInterface.makePostRequest(anyString(), any(), any(), any(),
            eq(NewMediaItemResult.class)))
        .thenReturn(mock(NewMediaItemResult.class));

    JobStore jobStore = new LocalJobStore();
    TemporaryPerJobDataStore dataStore = mock(TemporaryPerJobDataStore.class);

    InputStream inputStream = mock(InputStream.class);
    connectionProvider = mock(ConnectionProvider.class);
    InputStreamWrapper is = new InputStreamWrapper(inputStream, 32L);
    Mockito.when(connectionProvider.getInputStreamForItem(any(), any())).thenReturn(is);
    photosLibraryClient = mock(PhotosLibraryClient.class);

    googlePhotosImporter =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            dataStore,
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);
  }

  @Test
  public void importAlbum() throws Exception {
    // Set up
    String albumName = "Album Name";
    String albumDescription = "Album description";
    MediaAlbum albumModel = new MediaAlbum(OLD_ALBUM_ID, albumName, albumDescription);

    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setId(NEW_ALBUM_ID);
    Mockito.when(googlePhotosInterface.createAlbum(any(GoogleAlbum.class)))
        .thenReturn(responseAlbum);

    // Run test
    googlePhotosImporter.importSingleAlbum(uuid, null, albumModel);

    // Check results
    ArgumentCaptor<GoogleAlbum> albumArgumentCaptor = ArgumentCaptor.forClass(GoogleAlbum.class);
    Mockito.verify(googlePhotosInterface).createAlbum(albumArgumentCaptor.capture());
    assertEquals(albumArgumentCaptor.getValue().getTitle(), albumName);
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
            false,
            SHA1);
    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(SHA1))).thenReturn("token1");

    PhotoModel photoModel2 =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID2",
            OLD_ALBUM_ID,
            false);
    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null)))
        .thenReturn("token2");

    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[]{
                buildMediaItemResult("token1", Code.OK_VALUE),
                buildMediaItemResult("token2", Code.OK_VALUE)
            });
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    long length = googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel1, photoModel2),
        executor, UUID.randomUUID(), mock(TokensAndUrlAuthData.class));
    // Two photos of 32L each imported
    assertEquals(64L, length);
    assertTrue(executor.isKeyCached(String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID1")));
    assertTrue(executor.isKeyCached(String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID2")));
  }

  private NewMediaItemResult buildMediaItemResult(String uploadToken, int code) {
    // We do a lot of mocking as building the actual objects would require changing the constructors
    // which messed up deserialization so best to leave them unchanged.
    GoogleMediaItem mediaItem = mock(GoogleMediaItem.class);
    Mockito.when(mediaItem.getId()).thenReturn("newId");
    Status status = mock(Status.class);
    Mockito.when(status.getCode()).thenReturn(code);
    NewMediaItemResult result = mock(NewMediaItemResult.class);
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

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null)))
        .thenReturn("token1", "token2");
    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[]{
                buildMediaItemResult("token1", Code.OK_VALUE),
                buildMediaItemResult("token2", Code.UNAUTHENTICATED_VALUE)
            });
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    long length = googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel1, photoModel2),
        executor, UUID.randomUUID(), mock(TokensAndUrlAuthData.class));
    // Only one photo of 32L imported
    assertEquals(32L, length);

    assertTrue(executor.isKeyCached(String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID1")));
    String failedDataId = String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID2");
    assertFalse(executor.isKeyCached(failedDataId));
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals(failedDataId, errorDetail.id());
    assertThat(
        errorDetail.exception(), CoreMatchers.containsString("Media item could not be created."));
  }

  @Test
  public void importOnePhotoWithHashMismatch() throws Exception {
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            false,
            SHA1);

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(SHA1)))
        .thenThrow(new UploadErrorException("Hash mismatch will be thrown", new Throwable()));
    BatchMediaItemResponse batchMediaItemResponse = new BatchMediaItemResponse(
        new NewMediaItemResult[]{});
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    // No photo imported and will return a hash mismatch error for investigation.
    assertThrows(UploadErrorException.class,
        () -> googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel), executor,
            UUID.randomUUID(), mock(TokensAndUrlAuthData.class)));

    String failedDataId = String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID1");
    assertFalse(executor.isKeyCached(failedDataId));

    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals(failedDataId, errorDetail.id());
    assertThat(
        errorDetail.exception(), CoreMatchers.containsString("Hash mismatch"));
  }

  @Test
  public void importAlbumWithITString()
      throws PermissionDeniedException, InvalidTokenException, IOException, UploadErrorException {
    String albumId = "Album Id";
    String albumName = "Album Name";
    String albumDescription = "Album Description";

    MediaAlbum albumModel = new MediaAlbum(albumId, albumName, albumDescription);

    PortabilityJob portabilityJob = mock(PortabilityJob.class);
    Mockito.when(portabilityJob.userLocale()).thenReturn("it");
    JobStore jobStore = mock(JobStore.class);
    Mockito.when(jobStore.findJob(uuid)).thenReturn(portabilityJob);
    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setId(NEW_ALBUM_ID);
    Mockito.when(googlePhotosInterface.createAlbum(any(GoogleAlbum.class)))
        .thenReturn(responseAlbum);
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    GoogleMediaImporter sut =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);

    sut.importSingleAlbum(uuid, null, albumModel);
    ArgumentCaptor<GoogleAlbum> albumArgumentCaptor = ArgumentCaptor.forClass(GoogleAlbum.class);
    Mockito.verify(googlePhotosInterface).createAlbum(albumArgumentCaptor.capture());
    assertEquals(albumArgumentCaptor.getValue().getTitle(), albumName);
  }

  @Test
  public void retrieveAlbumStringOnlyOnce()
      throws PermissionDeniedException, InvalidTokenException, IOException, UploadErrorException {
    String albumId = "Album Id";
    String albumName = "Album Name";
    String albumDescription = "Album Description";

    MediaAlbum albumModel = new MediaAlbum(albumId, albumName, albumDescription);

    PortabilityJob portabilityJob = mock(PortabilityJob.class);
    Mockito.when(portabilityJob.userLocale()).thenReturn("it");
    JobStore jobStore = mock(JobStore.class);
    Mockito.when(jobStore.findJob(uuid)).thenReturn(portabilityJob);
    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setId(NEW_ALBUM_ID);
    Mockito.when(googlePhotosInterface.createAlbum(any(GoogleAlbum.class)))
        .thenReturn(responseAlbum);
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);

    GoogleMediaImporter sut =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);

    sut.importSingleAlbum(uuid, null, albumModel);
    sut.importSingleAlbum(uuid, null, albumModel);
    Mockito.verify(jobStore, atMostOnce()).findJob(uuid);
  }

  @Test
  public void importPhotoInTempStore() throws Exception {
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            true);

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null))).thenReturn("token1");
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);
    JobStore jobStore = mock(LocalJobStore.class);
    Mockito.when(jobStore.getStream(any(), any()))
        .thenReturn(
            new TemporaryPerJobDataStore.InputStreamWrapper(
                new ByteArrayInputStream("TestingBytes".getBytes())));
    Mockito.doNothing().when(jobStore).removeData(any(), anyString());

    ConnectionProvider connectionProvider = new ConnectionProvider(jobStore);
    GoogleMediaImporter googlePhotosImporter =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);

    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[]{buildMediaItemResult("token1", Code.OK_VALUE)});

    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    UUID jobId = UUID.randomUUID();

    googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel), executor, jobId,
        mock(TokensAndUrlAuthData.class));
    assertTrue(executor.isKeyCached(String.format("%s-%s", OLD_ALBUM_ID, "oldPhotoID1")));
    Mockito.verify(jobStore, Mockito.times(1)).removeData(any(), anyString());
    Mockito.verify(jobStore, Mockito.times(1)).getStream(any(), anyString());
  }

  @Test
  public void importPhotoInTempStoreFailure() throws Exception {
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            true);

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null)))
        .thenThrow(new IOException("Unit Testing"));
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);
    JobStore jobStore = mock(LocalJobStore.class);
    Mockito.when(jobStore.getStream(any(), any()))
        .thenReturn(
            new TemporaryPerJobDataStore.InputStreamWrapper(
                new ByteArrayInputStream("TestingBytes".getBytes())));
    Mockito.doNothing().when(jobStore).removeData(any(), anyString());

    ConnectionProvider connectionProvider = new ConnectionProvider(jobStore);
    GoogleMediaImporter googlePhotosImporter =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);

    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(
            new NewMediaItemResult[] {buildMediaItemResult("token1", Code.OK_VALUE)});

    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenReturn(batchMediaItemResponse);

    UUID jobId = UUID.randomUUID();

    googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel), executor, jobId,
        mock(TokensAndUrlAuthData.class));
    Mockito.verify(jobStore, Mockito.times(0)).removeData(any(), anyString());
    Mockito.verify(jobStore, Mockito.times(1)).getStream(any(), anyString());
  }

  @Test
  public void importPhotoFailedToFindAlbum() throws Exception {
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            true);

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null)))
        .thenReturn("token1", "token2");
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);
    JobStore jobStore = mock(LocalJobStore.class);
    Mockito.when(jobStore.getStream(any(), any()))
        .thenReturn(
            new TemporaryPerJobDataStore.InputStreamWrapper(
                new ByteArrayInputStream("TestingBytes".getBytes())));
    googlePhotosImporter =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);
    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenThrow(new IOException("The provided ID does not match any albums"));

    GoogleAlbum responseAlbum = new GoogleAlbum();
    Mockito.when(googlePhotosInterface.getAlbum(any())).thenReturn(responseAlbum);

    long bytes = googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel), executor, uuid,
        mock(TokensAndUrlAuthData.class));

    // didn't throw
    assertEquals(0, bytes);
  }

  @Test
  public void importPhotoCreatePhotosOtherException() throws Exception {
    PhotoModel photoModel =
        new PhotoModel(
            PHOTO_TITLE,
            IMG_URI,
            PHOTO_DESCRIPTION,
            JPEG_MEDIA_TYPE,
            "oldPhotoID1",
            OLD_ALBUM_ID,
            true);

    Mockito.when(googlePhotosInterface.uploadMediaContent(any(), eq(null)))
        .thenReturn("token1", "token2");
    PhotosLibraryClient photosLibraryClient = mock(PhotosLibraryClient.class);
    JobStore jobStore = mock(LocalJobStore.class);
    Mockito.when(jobStore.getStream(any(), any()))
        .thenReturn(
            new TemporaryPerJobDataStore.InputStreamWrapper(
                new ByteArrayInputStream("TestingBytes".getBytes())));
    googlePhotosImporter =
        new GoogleMediaImporter(
            null,  /*credentialFactory*/
            jobStore,
            mock(TemporaryPerJobDataStore.class),
            null,  /*jsonFactory*/
            null,  /*photosInterfacesMap*/
            googlePhotosInterface,
            photosLibraryClient,
            connectionProvider,
            monitor,
            1.0  /*writesPerSecond*/);

    Mockito.when(googlePhotosInterface.createPhotos(any(NewMediaItemUpload.class)))
        .thenThrow(new IOException("Some other exception"));

    GoogleAlbum responseAlbum = new GoogleAlbum();
    Mockito.when(googlePhotosInterface.getAlbum(any())).thenReturn(responseAlbum);

    assertThrows(IOException.class,
        () -> googlePhotosImporter.importPhotos(Lists.newArrayList(photoModel), executor, uuid,
            mock(TokensAndUrlAuthData.class)));
  }
}
