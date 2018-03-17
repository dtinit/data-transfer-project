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
package org.dataportabilityproject.dataModels.photos;

import com.google.common.base.MoreObjects;
import java.io.Serializable;

public class PhotoAlbum implements Serializable {

  private final String id;
  private final String name;
  private final String description;

  /** The {@code id} is used to associate photos with this album. */
  public PhotoAlbum(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!PhotoAlbum.class.isAssignableFrom(object.getClass())) {
      return false;
    }
    PhotoAlbum album = (PhotoAlbum) object;
    return this.description.equals(album.getDescription())
        && this.id.equals(album.getId())
        && this.name.equals(album.getName());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("name", name)
        .add("description", description)
        .toString();
  }
}
