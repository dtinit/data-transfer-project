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

package org.datatransferproject.transfer.facebook.videos;

import static org.mockito.Mockito.verify;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.GraphResponse;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FacebookVideosImporterTest {

  private static final String VIDEO_URL = "https://www.example.com/photo.jpg";
  private static final String VIDEO_DESCRIPTION = "description";

  private FacebookVideosImporter importer = new FacebookVideosImporter(null);
  @Mock private FacebookClient client;

  @Test
  public void testImportSingleVideo() {
    importer.importSingleVideo(
        client,
        new VideoObject(
            "title", VIDEO_URL, VIDEO_DESCRIPTION, "video/mp4", "videoId", null, false));

    Parameter[] params = {
      Parameter.with("file_url", VIDEO_URL), Parameter.with("description", VIDEO_DESCRIPTION)
    };
    verify(client).publish("me/videos", GraphResponse.class, params);
  }
}
