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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.types.common.models.ContainerResource;

import java.util.Collection;
import java.util.Objects;

public class VideosContainerResource extends ContainerResource {
  private final Collection<VideoAlbum> albums;
  private final Collection<VideoObject> videos;

  @JsonCreator
  public VideosContainerResource(
          @JsonProperty("albums") Collection<VideoAlbum> albums,
          @JsonProperty("videos") Collection<VideoObject> videos) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.videos = videos == null ? ImmutableList.of() : videos;
  }

  public Collection<VideoAlbum> getAlbums() {
    return albums;
  }

  public Collection<VideoObject> getVideos() {
    return videos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VideosContainerResource that = (VideosContainerResource) o;
    return Objects.equals(getAlbums(), that.getAlbums()) &&
            Objects.equals(getVideos(), that.getVideos());
  }
}
