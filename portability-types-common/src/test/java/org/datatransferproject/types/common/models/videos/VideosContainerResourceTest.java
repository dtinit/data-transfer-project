/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models.videos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.datatransferproject.types.common.models.ContainerResource;
import org.junit.Test;

import java.util.List;

public class VideosContainerResourceTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(VideosContainerResource.class);

    List<VideoAlbum> albums =
            ImmutableList.of(new VideoAlbum("id1", "album1", "This is a fake album"));

    List<VideoObject> videos =
            ImmutableList.of(
                    new VideoObject("Vid1", "http://example.com/1.mp4", "A video", "video/mp4", "v1", "id1",
                            false),
                    new VideoObject(
                            "Vid2", "https://example.com/2.mpeg", "A 2. video", "video/mpeg", "v2", "id1", false));
    
    ContainerResource data = new VideosContainerResource(albums, videos);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
            objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(VideosContainerResource.class);
    VideosContainerResource deserialized = (VideosContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getAlbums()).hasSize(1);
    Truth.assertThat(deserialized.getVideos()).hasSize(2);
  }
}
