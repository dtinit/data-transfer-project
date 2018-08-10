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
package org.datatransferproject.transfer.smugmug.photos.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SmugMugModelTest {

  private ObjectMapper objectMapper;

  String caption = "Caption";
  String title = "Title";
  String fileName = "file.name";

  SmugMugImage imageIn;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(SmugMugImage.class, SmugMugAlbumImage.class);

    imageIn = new SmugMugImage();
    imageIn.setCaption(caption);
    imageIn.setTitle(title);
    imageIn.setFileName(fileName);
  }

  @Test
  public void testImageMapping() throws IOException {
    String serialized = objectMapper.writeValueAsString(imageIn);

    SmugMugImage imageOut = objectMapper.readValue(serialized, SmugMugImage.class);

    Assert.assertNotNull(imageOut);
    Assert.assertEquals(imageIn.getCaption(), imageOut.getCaption());
    Assert.assertEquals(imageIn.getTitle(), imageOut.getTitle());
    Assert.assertEquals(imageIn.getFileName(), imageOut.getFileName());
  }

  @Test
  public void testAlbumImageMapping() {
    
  }
}
