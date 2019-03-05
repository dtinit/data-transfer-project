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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
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

  private static final String OLD_ALBUM_ID = "OLD_ALBUM_ID";
  private static final String NEW_ALBUM_ID = "NEW_ALBUM_ID";

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

    googlePhotosImporter = new GooglePhotosImporter(null, jobStore, null,
        googlePhotosInterface, imageStreamProvider);
  }

  @Test
  public void exportAlbum() throws IOException {
    // Set up
    String albumName = "Album Name";
    String albumDescription = "Album description";
    PhotoAlbum albumModel = new PhotoAlbum(OLD_ALBUM_ID, albumName, albumDescription);

    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setId(NEW_ALBUM_ID);
    when(googlePhotosInterface.createAlbum(Matchers.any(GoogleAlbum.class)))
        .thenReturn(responseAlbum);

    // Run test
    googlePhotosImporter.importSingleAlbum(uuid, null, albumModel);

    // Check results
    ArgumentCaptor<GoogleAlbum> albumArgumentCaptor = ArgumentCaptor.forClass(GoogleAlbum.class);
    verify(googlePhotosInterface).createAlbum(albumArgumentCaptor.capture());
    assertEquals(albumArgumentCaptor.getValue().getTitle(), "Copy of " + albumName);
    assertNull(albumArgumentCaptor.getValue().getId());

    TempPhotosData tempPhotosData = jobStore.findData(uuid, "tempPhotosData", TempPhotosData.class);
    assertEquals(tempPhotosData.lookupNewAlbumId(OLD_ALBUM_ID), NEW_ALBUM_ID);
  }

  @Test
  public void exportPhoto() throws IOException {
    // Set up
    PhotoModel photoModel = new PhotoModel(PHOTO_TITLE, IMG_URI, PHOTO_DESCRIPTION, JPEG_MEDIA_TYPE,
        null, OLD_ALBUM_ID, false);
    TempPhotosData tempPhotosData = new TempPhotosData(uuid);
    tempPhotosData.addAlbumId(OLD_ALBUM_ID, NEW_ALBUM_ID);
    jobStore.create(uuid, "tempPhotosData", tempPhotosData);

    // Run test
    googlePhotosImporter.importSinglePhoto(uuid, null, photoModel);

    // Check results
    verify(imageStreamProvider).get(IMG_URI);
    verify(googlePhotosInterface).uploadPhotoContent(inputStream);

    ArgumentCaptor<NewMediaItemUpload> uploadArgumentCaptor = ArgumentCaptor
        .forClass(NewMediaItemUpload.class);
    verify(googlePhotosInterface).createPhoto(uploadArgumentCaptor.capture());
    assertEquals(uploadArgumentCaptor.getValue().getAlbumId(), NEW_ALBUM_ID);
    List<NewMediaItem> newMediaItems = uploadArgumentCaptor.getValue().getNewMediaItems();
    assertEquals(newMediaItems.size(), 1);
    NewMediaItem mediaItem = newMediaItems.get(0);
    assertEquals(mediaItem.getSimpleMediaItem().getUploadToken(), UPLOAD_TOKEN);
    assertEquals(mediaItem.getDescription(), "Copy of " + PHOTO_DESCRIPTION);
  }
}
