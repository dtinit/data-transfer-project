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
package org.dataportabilityproject.datatransfer.google.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.datatransfer.google.photos.GooglePhotosImporter.DEFAULT_ALBUM_ID;
import static org.dataportabilityproject.datatransfer.google.photos.GooglePhotosImporter.PHOTO_POST_URL_FORMATTER;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.transfer.ImageStreamProvider;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class GooglePhotosImporterTest {

  private String PHOTO_TITLE = "Model photo title";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosImporter googlePhotosImporter;
  private PicasawebService photoService;
  private ImageStreamProvider imageStreamProvider;
  private InputStream inputStream;

  @Before
  public void setUp() throws IOException, ServiceException {
    photoService = mock(PicasawebService.class);

    inputStream = mock(InputStream.class);
    imageStreamProvider = mock(ImageStreamProvider.class);
    when(imageStreamProvider.get(Matchers.anyString())).thenReturn(inputStream);

    googlePhotosImporter = new GooglePhotosImporter(null, null, photoService, imageStreamProvider);
  }

  @Test
  public void exportPhoto() throws IOException, ServiceException {
    // Set up
    String description = "description";
    PhotoModel photoModel = new PhotoModel(PHOTO_TITLE, IMG_URI, description, JPEG_MEDIA_TYPE,
        "album_id");

    // Run test
    googlePhotosImporter.importSinglePhoto(null, photoModel);

    // Check results
    // Verify correct methods were called
    verify(imageStreamProvider).get(IMG_URI);

    URL expectedUploadURL = new URL(String.format(PHOTO_POST_URL_FORMATTER, DEFAULT_ALBUM_ID));
    ArgumentCaptor<PhotoEntry> photoEntryArgumentCaptor = ArgumentCaptor.forClass(PhotoEntry.class);
    verify(photoService).insert(eq(expectedUploadURL), photoEntryArgumentCaptor.capture());

    // Check that uploaded photo entry is as expected
    assertThat(photoEntryArgumentCaptor.getValue().getTitle().getPlainText())
        .isEqualTo("copy of " + PHOTO_TITLE);
    assertThat(photoEntryArgumentCaptor.getValue().getDescription().getPlainText())
        .isEqualTo(description);
    assertThat(photoEntryArgumentCaptor.getValue().getClient())
        .isEqualTo(GoogleStaticObjects.APP_NAME);
    assertThat(photoEntryArgumentCaptor.getValue().getMediaSource().getContentType())
        .isEqualTo(JPEG_MEDIA_TYPE);
  }
}
