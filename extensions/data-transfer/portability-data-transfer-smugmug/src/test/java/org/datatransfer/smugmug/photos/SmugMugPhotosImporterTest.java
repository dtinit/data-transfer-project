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

package org.datatransferproject.transfer.smugmug.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.BufferedInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.smugmug.SmugMugTransmogrificationConfig;
import org.datatransferproject.transfer.smugmug.photos.model.*;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugImageUploadResponse.ImageInfo;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SmugMugPhotosImporterTest {
  private static final String TEMP_DATA_FORMAT = "smugmug-album-temp-data-%s";
  private static final IdempotentImportExecutor EXECUTOR = new FakeIdempotentImportExecutor();

  private TemporaryPerJobDataStore jobStore = new LocalJobStore();

  private BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);

  private Monitor monitor = mock(Monitor.class);
  private SmugMugInterface smugMugInterface = mock(SmugMugInterface.class);

  private SmugMugTransmogrificationConfig config =
      new SmugMugTransmogrificationConfig() {
        public int getAlbumMaxSize() {
          return 2;
        }
      };

  @Test
  public void importStoresAlbumInJobStore() throws Exception {
    // setup test objects
    UUID jobId = UUID.randomUUID();

    PhotoAlbum photoAlbum1 = new PhotoAlbum("albumId1", "albumName1", "albumDescription1");
    PhotoModel photoModel1 =
        new PhotoModel(
            "PHOTO_TITLE",
            "FETCHABLE_URL",
            "PHOTO_DESCRIPTION",
            "MEDIA_TYPE",
            "photoId1",
            photoAlbum1.getId(),
            false);
    PhotoModel photoModel2 =
        new PhotoModel(
            "PHOTO_TITLE",
            "FETCHABLE_URL",
            "PHOTO_DESCRIPTION",
            "MEDIA_TYPE",
            "photoId2",
            photoAlbum1.getId(),
            false);
    PhotoModel photoModel3 =
        new PhotoModel(
            "PHOTO_TITLE",
            "FETCHABLE_URL",
            "PHOTO_DESCRIPTION",
            "MEDIA_TYPE",
            "photoId3",
            photoAlbum1.getId(),
            false);

    PhotosContainerResource photosContainerResource1 =
        new PhotosContainerResource(Collections.singletonList(photoAlbum1), ImmutableList.of());
    PhotosContainerResource photosContainerResource2 =
        new PhotosContainerResource(
            ImmutableList.of(), ImmutableList.of(photoModel1, photoModel2, photoModel3));


    SmugMugAlbum smugMugAlbum1 =
        new SmugMugAlbum(
            "date",
            photoAlbum1.getDescription(),
            "Copy of " + photoAlbum1.getName(),
            "privacy",
            "albumUri1",
            "urlname",
            "weburi");
    String overflowAlbumName = "Copy of " + smugMugAlbum1.getName() + " (1)";
    SmugMugAlbum smugMugAlbum2 =
        new SmugMugAlbum(
            "date",
            photoAlbum1.getDescription(),
            overflowAlbumName,
            "privacy",
            "albumUri2",
            "urlname",
            "weburi");

    SmugMugAlbumResponse mockAlbumResponse1 =
        new SmugMugAlbumResponse(smugMugAlbum1.getUri(), "Locator", "LocatorType", smugMugAlbum1);
    SmugMugAlbumResponse mockAlbumResponse2 =
        new SmugMugAlbumResponse(smugMugAlbum2.getUri(), "Locator", "LocatorType", smugMugAlbum2);


    when(smugMugInterface.createAlbum(eq(smugMugAlbum1.getName()))).thenReturn(mockAlbumResponse1);
    when(smugMugInterface.createAlbum(eq(smugMugAlbum2.getName()))).thenReturn(mockAlbumResponse2);

    SmugMugImageUploadResponse smugMugUploadImageResponse =
        new SmugMugImageUploadResponse(
            "imageUri",
            "albumImageUri",
            new ImageInfo("imageUri", "albumImageUri", "statusImageReplaceUri", "url"));
    when(smugMugInterface.uploadImage(any(), any(), any())).thenReturn(smugMugUploadImageResponse);
    when(smugMugInterface.getImageAsStream(any())).thenReturn(bufferedInputStream);

    // Run test
    SmugMugPhotosImporter importer =
        new SmugMugPhotosImporter(
            smugMugInterface,
            config,
            jobStore,
            new AppCredentials("key", "secret"),
            mock(ObjectMapper.class),
            monitor);
    ImportResult result =
        importer.importItem(
            jobId, EXECUTOR, new TokenSecretAuthData("token", "secret"), photosContainerResource1);

    result =
        importer.importItem(
            jobId, EXECUTOR, new TokenSecretAuthData("token", "secret"), photosContainerResource2);

    // Verify 
    ArgumentCaptor<String> photoUrlsCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> albumNamesCaptor = ArgumentCaptor.forClass(String.class);
    verify(smugMugInterface, atLeastOnce()).createAlbum(albumNamesCaptor.capture());
    verify(smugMugInterface, atLeastOnce()).getImageAsStream(photoUrlsCaptor.capture());
    
    List<String> capturedAlbumNames = albumNamesCaptor.getAllValues();
    assertTrue(capturedAlbumNames.contains(smugMugAlbum1.getName()));
    assertTrue(capturedAlbumNames.contains(smugMugAlbum2.getName()));
    
    List<String> capturedPhotoUrls = photoUrlsCaptor.getAllValues();
    assertTrue(capturedPhotoUrls.contains(photoModel1.getFetchableUrl()));
    assertTrue(capturedPhotoUrls.contains(photoModel2.getFetchableUrl()));
    assertTrue(capturedPhotoUrls.contains(photoModel3.getFetchableUrl()));

    String overflowAlbumId = photoAlbum1.getId() + "-overflow-1";
    assertThat((String) EXECUTOR.getCachedValue(photoAlbum1.getId()))
        .isEqualTo(smugMugAlbum1.getUri());
    assertThat((String) EXECUTOR.getCachedValue(overflowAlbumId)).isEqualTo(smugMugAlbum2.getUri());

    SmugMugPhotoTempData tempData1 =
        new SmugMugPhotoTempData(
            photoAlbum1.getId(),
            smugMugAlbum1.getName(),
            smugMugAlbum1.getDescription(),
            smugMugAlbum1.getUri(),
            2,
            overflowAlbumId);
    SmugMugPhotoTempData tempData2 =
        new SmugMugPhotoTempData(
            overflowAlbumId,
            smugMugAlbum2.getName(),
            smugMugAlbum2.getDescription(),
            smugMugAlbum2.getUri(),
            1,
            null);
    assertThat(
            jobStore
                .findData(
                    jobId,
                    String.format(TEMP_DATA_FORMAT, photoAlbum1.getId()),
                    SmugMugPhotoTempData.class)
                .toString())
        .isEqualTo(tempData1.toString());
    assertThat(
            jobStore
                .findData(
                    jobId,
                    String.format(TEMP_DATA_FORMAT, overflowAlbumId),
                    SmugMugPhotoTempData.class)
                .toString())
        .isEqualTo(tempData2.toString());
  }

  @Test
  public void importEmptyAlbumName() throws Exception{
    UUID jobId = UUID.randomUUID();
    PhotoAlbum photoAlbum = new PhotoAlbum("albumid", "", "albumDescription");
    PhotosContainerResource photosContainerResource =
        new PhotosContainerResource(Collections.singletonList(photoAlbum), ImmutableList.of());

    SmugMugAlbum smugMugAlbum =
        new SmugMugAlbum(
            "date",
            photoAlbum.getDescription(),
            "Untitled Album",
            "privacy",
            "albumUri1",
            "urlname",
            "weburi");
    SmugMugAlbumResponse mockAlbumResponse =
        new SmugMugAlbumResponse(smugMugAlbum.getUri(), "Locator", "LocatorType", smugMugAlbum);
    when(smugMugInterface.createAlbum(eq(smugMugAlbum.getName()))).thenReturn(mockAlbumResponse);

    // Run test
    SmugMugPhotosImporter importer =
        new SmugMugPhotosImporter(
            smugMugInterface,
            config,
            jobStore,
            new AppCredentials("key", "secret"),
            mock(ObjectMapper.class),
            monitor);
    ImportResult result =
        importer.importItem(
            jobId, EXECUTOR, new TokenSecretAuthData("token", "secret"), photosContainerResource);

    // Verify
    verify(smugMugInterface, atLeastOnce()).createAlbum(ArgumentCaptor.forClass(String.class).capture());
  }
}
