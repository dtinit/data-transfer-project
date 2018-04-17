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

import static org.mockito.Mockito.mock;

import com.google.gdata.client.photos.PicasawebService;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;

public class GooglePhotosImporterTest {

  private String PHOTO_TITLE = "Model photo title";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosImporter googlePhotosImporter;
  private PicasawebService photoService;
  private GoogleCredentialFactory credentialFactory;

  @Before
  public void setUp() {
    photoService = mock(PicasawebService.class);
    googlePhotosImporter = new GooglePhotosImporter(null, null, photoService);
  }

  @Test
  public void exportPhoto() {
    PhotoModel photoModel = new PhotoModel(PHOTO_TITLE, IMG_URI, "description", JPEG_MEDIA_TYPE,
        "album_id");

  }
}
