/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * TempPhotosData used to store album and photos information before they are ready to be uploaded.
 */
@JsonTypeName("org.dataportability:TempPhotosData")
public class TempPhotosData extends DataModel {

  @JsonProperty("jobId")
  private final UUID jobId;

  // Map of PhotoAlbums keyed by Album name.
  @JsonProperty("tempPhotoAlbums")
  private final Map<String, PhotoAlbum> tempPhotoAlbums;

  // Map of newAlbumIds keyed by the old album name.
  @JsonProperty("newAlbumIds")
  private final Map<String, String> newAlbumIds;

  public TempPhotosData(
      @JsonProperty("jobId") UUID jobId,
      @JsonProperty("tempPhotoAlbums") Map<String, PhotoAlbum> tempPhotoAlbums,
      @JsonProperty("newAlbumIds") Map<String, String> newAlbumIds) {
    this.jobId = jobId;
    this.tempPhotoAlbums = tempPhotoAlbums;
    this.newAlbumIds = newAlbumIds;
  }

  public TempPhotosData(@JsonProperty("jobId") UUID jobId) {
    this.jobId = jobId;
    this.tempPhotoAlbums = new HashMap<>();
    this.newAlbumIds = new HashMap<>();
  }

  // Adds the <Key, PhotoAlbum> mapping provided
  public void addTempAlbumMapping(String key, PhotoAlbum album) {
    tempPhotoAlbums.put(key, album);
  }

  // Looks up the PhotoAlbum corresponding to the key provided
  public PhotoAlbum lookupTempAlbum(String key) {
    return tempPhotoAlbums.getOrDefault(key, null);
  }

  // Adds a mapping from old album id to new album id
  public void addAlbumId(String oldAlbumId, String newAlbumId) {
    newAlbumIds.put(oldAlbumId, newAlbumId);
  }

  // returns the album id mapped to by old album id
  public String lookupNewAlbumId(String oldAlbumId) {
    return newAlbumIds.getOrDefault(oldAlbumId, "");
  }

  // removes the temp photo album
  public void removeTempPhotoAlbum(String key) {
    if (tempPhotoAlbums.containsKey(key)) {
      tempPhotoAlbums.remove(key);
    }
  }
}
