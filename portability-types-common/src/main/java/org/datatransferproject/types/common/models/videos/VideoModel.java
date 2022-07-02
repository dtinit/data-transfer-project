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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.Fileable;
import org.datatransferproject.types.common.FolderItem;
import org.datatransferproject.types.common.models.MediaObject;

import java.util.stream.Collectors;

public class VideoModel extends MediaObject implements Fileable, FolderItem, DownloadableItem {

  private String dataId;
  private String albumId;
  private boolean inTempStore;

  @JsonCreator
  public VideoModel(
          @JsonProperty("name") String name,
          @JsonProperty("contentUrl") String contentUrl,
          @JsonProperty("description") String description,
          @JsonProperty("encodingFormat") String encodingFormat,
          @JsonProperty("dataId") String dataId,
          @JsonProperty("albumId") String albumId,
          @JsonProperty("inTempStore") boolean inTempStore) {
    super(dataId);
    setName(name);
    setContentUrl(contentUrl);
    setDescription(description);
    setEncodingFormat(encodingFormat);
    this.dataId = dataId;
    this.albumId = albumId;
    this.inTempStore = inTempStore;
  }

  // TODO(zacsh) remove this in favor of getFolderId
  public String getAlbumId() {
    return albumId;
  }

  // requirement of FolderItem
  @JsonIgnore
  public String getFolderId() {
    return getAlbumId();
  }

  @JsonIgnore
  // requirement of Fileable
  public String getMimeType() {
    // TODO(zacsh) DO NOT MERGE - not everyone is using encodingFormat in this way - I swear I saw
    // some code using "UTF-8" as a string value. Fix and then do a better job documenting the
    // constructors of all Fileable models!
    return getEncodingFormat();
  }

  public String getDataId() {
    return dataId;
  }

  @Override
  public boolean isInTempStore() {
    return inTempStore;
  }

  @JsonIgnore
  @Override
  public String getFetchableUrl() {
    return getContentUrl().toString();
  }

  @JsonIgnore(false)
  @Override
  // TODO(zacsh) this and video--and all *many* models perhaps--should agree on an interface so we
  // don't have "getName()" vs "getTitle()" or "getFetchableUrl" that _happens_ to be the same
  // method name but only by sheer luck
  public String getName() {
    return super.getName();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("name", getName())
            .add("contentUrl", getContentUrl())
            .add("description", getDescription())
            .add("encodingFormat", getEncodingFormat())
            .add("dataId", dataId)
            .add("albumId", albumId)
            .add("inTempStore", inTempStore)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VideoModel that = (VideoModel) o;
    return Objects.equal(getName(), that.getName()) &&
            Objects.equal(getContentUrl(), that.getContentUrl()) &&
            Objects.equal(getDescription(), that.getDescription()) &&
            Objects.equal(getEncodingFormat(), that.getEncodingFormat()) &&
            Objects.equal(getDataId(), that.getDataId()) &&
            Objects.equal(getAlbumId(), that.getAlbumId());
  }

  // Assign this video to a different album. Used in cases where an album is too large and
  // needs to be divided into smaller albums, the videos will each get reassigned to new
  // albumnIds.
  public void reassignToAlbum(String newAlbum){
    this.albumId = newAlbum;
  }

  // remove all forbidden characters
  public void cleanName(String forbiddenCharacters, char replacementCharacter, int maxLength) {
    String name = getName().chars()
        .mapToObj(c -> (char) c)
        .map(c -> forbiddenCharacters.contains(Character.toString(c)) ? replacementCharacter : c)
        .map(Object::toString)
        .collect(Collectors.joining("")).trim();

    if (maxLength <= 0) {
      setName(name.trim());
      return;
    }

    setName(name.substring(0, Math.min(maxLength, name.length())).trim());
  }


  @Override
  public int hashCode() {
    return this.dataId.hashCode();
  }
}
