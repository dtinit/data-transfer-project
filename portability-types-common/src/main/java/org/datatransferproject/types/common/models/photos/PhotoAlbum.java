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
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PhotoAlbum {
  private final String id;
  private String name;
  private final String description;

  /** The {@code id} is used to associate photos with this album. * */
  @JsonCreator
  public PhotoAlbum(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description) {
    Preconditions.checkNotNull(id);
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("name", getName())
        .add("description", getDescription())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhotoAlbum that = (PhotoAlbum) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id);
  }

  // Generates PhotoAlbum objects that represent fragments of this one.
  // Used in cases where an album from the originating service is larger than the allowable size
  // in the destination service.
  // If an album "MyAlbum" is split into 3, the results will be "MyAlbum (1/3)", etc.
  public List<PhotoAlbum> split(int numberOfNewAlbums){
    List<PhotoAlbum> newAlbums = new ArrayList<>();
    for(int i = 1; i <= numberOfNewAlbums; i++){
      newAlbums.add(
        new PhotoAlbum(
          String.format("%s-pt%d", id, i),
          String.format("%s (%d/%d)", id, i, numberOfNewAlbums),
          description
        )
      );
    }
    return newAlbums;
  }

  // This allows us to make album names palatable, removing unpalatable characters and
  // enforcing length rules
  public void cleanName(String forbiddenCharacters, char replacementCharacter, int maxLength) {
    // An album name is allowed to be null, handled on the importer level if there is a problem with
    // this value, so we support it here
    if (name == null) {
      return;
    }
    name = name.chars()
        .mapToObj(c -> (char) c)
        .map(c -> forbiddenCharacters.contains(Character.toString(c)) ? replacementCharacter : c)
        .map(Object::toString)
        .collect(Collectors.joining("")).trim();
    if (maxLength <= 0 || maxLength >= name.length()) {
      return;
    }
    name = name.substring(0, maxLength).trim();
  }
}
