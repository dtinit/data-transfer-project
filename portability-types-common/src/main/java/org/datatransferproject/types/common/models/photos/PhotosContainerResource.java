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
package org.datatransferproject.types.common.models.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.types.common.models.ContainerResource;

import java.util.Collection;
import java.util.Objects;

/** A Wrapper for all the possible objects that can be returned by a photos exporter. */
@JsonTypeName("PhotosContainerResource")
public class PhotosContainerResource extends ContainerResource {
  private final Collection<PhotoAlbum> albums;
  private final Collection<PhotoModel> photos;

  @JsonCreator
  public PhotosContainerResource(
      @JsonProperty("albums") Collection<PhotoAlbum> albums,
      @JsonProperty("photos") Collection<PhotoModel> photos) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.photos = photos == null ? ImmutableList.of() : photos;
  }

  public Collection<PhotoAlbum> getAlbums() {
    return albums;
  }

  public Collection<PhotoModel> getPhotos() {
    return photos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhotosContainerResource that = (PhotosContainerResource) o;
    return Objects.equals(getAlbums(), that.getAlbums()) &&
            Objects.equals(getPhotos(), that.getPhotos());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAlbums(), getPhotos());
  }
}
