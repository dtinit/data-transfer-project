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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class GooglePhotosImporterTest {

  private String PHOTO_TITLE = "Model photo title";
  private String PHOTO_DESCRIPTION = "Model photo description";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";
  private String UPLOAD_TOKEN = "uploadToken";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosImporter googlePhotosImporter;
  private GooglePhotosInterface googlePhotosInterface;
  private JobStore jobStore;
  private ImageStreamProvider imageStreamProvider;
  private InputStream inputStream;

  @Before
  public void setUp() throws IOException {
    googlePhotosInterface = mock(GooglePhotosInterface.class);
    when(googlePhotosInterface.uploadPhotoContent(Matchers.any(InputStream.class)))
        .thenReturn(UPLOAD_TOKEN);
    when(googlePhotosInterface.makePostRequest(Matchers.anyString(), Matchers.any(), Matchers.any(),
        Matchers.eq(NewMediaItemResult.class))).thenReturn(mock(NewMediaItemResult.class));

    jobStore = new LocalJobStore();

    inputStream = mock(InputStream.class);
    imageStreamProvider = mock(ImageStreamProvider.class);
    when(imageStreamProvider.get(Matchers.anyString())).thenReturn(inputStream);

    googlePhotosImporter = new GooglePhotosImporter(null, jobStore, googlePhotosInterface,
        imageStreamProvider);
  }

  @Test
  public void exportPhoto() throws IOException, ServiceException {
    // Set up
    String oldAlbumId = "oldAlbumId";
    String newAlbumId = "newAlbumId";
    PhotoModel photoModel = new PhotoModel(PHOTO_TITLE, IMG_URI, PHOTO_DESCRIPTION, JPEG_MEDIA_TYPE,
        null, oldAlbumId, false);
    TempPhotosData tempPhotosData = new TempPhotosData(uuid);
    tempPhotosData.addAlbumId(oldAlbumId, newAlbumId);
    jobStore.create(uuid, "tempPhotosData", tempPhotosData);

    // Run test
    googlePhotosImporter.importSinglePhoto(null, photoModel, uuid);

    // Check results
    // Verify correct methods were called
    verify(imageStreamProvider).get(IMG_URI);
    verify(googlePhotosInterface).uploadPhotoContent(inputStream);

    ArgumentCaptor<NewMediaItemUpload> argument = ArgumentCaptor.forClass(NewMediaItemUpload.class);
    verify(googlePhotosInterface).createPhoto(argument.capture());
    assertEquals(argument.getValue().getAlbumId(), newAlbumId);
    List<NewMediaItem> newMediaItems = argument.getValue().getNewMediaItems();
    assertEquals(newMediaItems.size(), 1);
    NewMediaItem mediaItem = newMediaItems.get(0);
    assertEquals(mediaItem.getSimpleMediaItem().getUploadToken(), UPLOAD_TOKEN);
    assertEquals(mediaItem.getDescription(), "Copy of " + PHOTO_DESCRIPTION);
  }
}
