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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.datatransferproject.types.common.DownloadableFile;

public class PhotoModel implements DownloadableFile {

  private String title;
  private final String fetchableUrl;
  private final String description;
  private final String mediaType;
  private String albumId;
  private final boolean inTempStore;
  private String dataId;
  @Nullable private String sha1;  // SHA-1 hash in Hex (base16).
  private Date uploadedTime;

  @JsonCreator
  public PhotoModel(
      @JsonProperty("title") String title,
      @JsonProperty("fetchableUrl") String fetchableUrl,
      @JsonProperty("description") String description,
      @JsonProperty("mediaType") String mediaType,
      @JsonProperty("dataId") String dataId,
      @JsonProperty("albumId") String albumId,
      @JsonProperty("inTempStore") boolean inTempStore,
      @Nullable @JsonProperty("sha1") String sha1,
      @JsonProperty("uploadedTime") Date uploadedTime) {
    this.title = title;
    this.fetchableUrl = fetchableUrl;
    this.description = description;
    this.mediaType = mediaType;
    if (dataId == null || dataId.isEmpty()) {
      throw new IllegalArgumentException("dataID must be set");
    }
    this.dataId = dataId;
    this.albumId = albumId;
    this.inTempStore = inTempStore;
    this.sha1 = sha1;
    this.uploadedTime = uploadedTime;
  }

  public PhotoModel(
      String title,
      String fetchableUrl,
      String description,
      String mediaType,
      String dataId,
      String albumId,
      boolean inTempStore,
      Date uploadedTime) {
    this(
        title,
        fetchableUrl,
        description,
        mediaType,
        dataId,
        albumId,
        inTempStore,
        /* sha1= */ null,
        uploadedTime);
  }

  public PhotoModel(
      String title,
      String fetchableUrl,
      String description,
      String mediaType,
      String dataId,
      String albumId,
      boolean inTempStore) {
    this(
        title,
        fetchableUrl,
        description,
        mediaType,
        dataId,
        albumId,
        inTempStore,
        /* sha1= */ null,
        /* uploadedTime= */ null);
  }

  public PhotoModel(
      String title,
      String fetchableUrl,
      String description,
      String mediaType,
      String dataId,
      String albumId,
      boolean inTempStore,
      String sha1) {
    this(
        title,
        fetchableUrl,
        description,
        mediaType,
        dataId,
        albumId,
        inTempStore,
        sha1,
        /*uploadedTime=*/ null);
  }

  // TODO(zacsh) convert all callers to ImportableItem#getName() which is an interface guarantee of this class's
  // being a DownloadableItem. Then delete this method.
  public String getTitle() {
    return title;
  }

  @Override
  public String getFetchableUrl() {
    return fetchableUrl;
  }

  public String getDescription() {
    return description;
  }

  // TODO(zacsh) remove this in favor of getMimeType
  public String getMediaType() {
    return mediaType;
  }

  @JsonIgnore
  // requirement of Fileable
  public String getMimeType() {
    return getMediaType();
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

  public String getDataId() {
    return dataId;
  }

  public Date getUploadedTime() {
    return uploadedTime;
  }

  @Nullable
  public String getSha1() {
    return sha1;
  }

  // remove all forbidden characters
  public void cleanTitle(String forbiddenCharacters, char replacementCharacter, int maxLength) {
    title = title.chars()
        .mapToObj(c -> (char) c)
        .map(c -> forbiddenCharacters.contains(Character.toString(c)) ? replacementCharacter : c)
        .map(Object::toString)
        .collect(Collectors.joining("")).trim();
    if (maxLength <= 0) {
      return;
    }
    title = title.substring(0, Math.min(maxLength, title.length())).trim();
  }

  @Override
  public boolean isInTempStore() { return inTempStore; }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("title", title)
        .add("fetchableUrl", fetchableUrl)
        .add("description", description)
        .add("mediaType", mediaType)
        .add("dataId", dataId)
        .add("albumId", albumId)
        .add("inTempStore", inTempStore)
        .add("sha1", sha1)
        .add("uploadedTime", uploadedTime)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhotoModel that = (PhotoModel) o;
    return Objects.equal(getTitle(), that.getTitle()) &&
            Objects.equal(getFetchableUrl(), that.getFetchableUrl()) &&
            Objects.equal(getDescription(), that.getDescription()) &&
            Objects.equal(getMediaType(), that.getMediaType()) &&
            Objects.equal(getDataId(), that.getDataId()) &&
            Objects.equal(getAlbumId(), that.getAlbumId()) &&
            Objects.equal(getSha1(), that.getSha1()) &&
            Objects.equal(getUploadedTime(), that.getUploadedTime());
  }

  @Override
  public int hashCode() {
    return this.dataId.hashCode();
  }

  // Assign this photo to a different album. Used in cases where an album is too large and
  // needs to be divided into smaller albums, the photos will each get reassigned to new
  // albumnIds.
  public void reassignToAlbum(String newAlbum){
    this.albumId = newAlbum;
  }

  @Nonnull
  @Override
  public String getIdempotentId() {
    return getAlbumId() + "-" + getDataId();
  }

  @Nullable
  @Override
  // required for org.datatransferproject.types.common.ImportableItem
  public String getName() {
    return getTitle();
  }
}
