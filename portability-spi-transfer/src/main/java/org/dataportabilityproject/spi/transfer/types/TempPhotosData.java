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

/*
 * TempPhotosData used to store album and photos information before they are ready to be uploaded.
 */
@JsonTypeName("org.dataportability:TempPhotosData")
public class TempPhotosData extends DataModel {

  @JsonProperty("jobId")
  private final String jobId;

  // Map of PhotoAlbums keyed by Album name.
  @JsonProperty("photoAlbums")
  private final Map<String, PhotoAlbum> photoAlbums;

  // Map of newAlbumIds keyed by the old album name.
  @JsonProperty("newAlbumIds")
  private final Map<String, String> newAlbumIds;

  public TempPhotosData(
      @JsonProperty("jobId") String jobId,
      @JsonProperty("albums") Map<String, PhotoAlbum> photoAlbums,
      @JsonProperty("newAlbumIds") Map<String, String> newAlbumIds) {
    this.jobId = jobId;
    this.photoAlbums = photoAlbums;
    this.newAlbumIds = newAlbumIds;
  }

  public TempPhotosData(@JsonProperty("jobId") String jobId) {
    this.jobId = jobId;
    this.photoAlbums = new HashMap<>();
    this.newAlbumIds = new HashMap<>();
  }

  // Adds the <Key, PhotoAlbum> mapping provided
  public void addAlbum(String key, PhotoAlbum album) {
    photoAlbums.put(key, album);
  }

  // Looks up the PhotoAlbum corresponding to the key provided
  public PhotoAlbum lookupAlbum(String key) {
    return photoAlbums.getOrDefault(key, null);
  }

  // Adds a mapping from old album id to new album id
  public void addAlbumId(String oldAlbumId, String newAlbumId) {
    newAlbumIds.put(oldAlbumId, newAlbumId);
  }

  // returns the album id mapped to by old album id
  public String lookupNewAlbumId(String oldAlbumId) {
    return newAlbumIds.getOrDefault(oldAlbumId, "");
  }
}
