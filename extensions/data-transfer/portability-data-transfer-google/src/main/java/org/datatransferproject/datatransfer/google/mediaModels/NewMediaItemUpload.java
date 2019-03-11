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

package org.datatransferproject.datatransfer.google.mediaModels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class containing all the information necessary to create new {@link NewMediaItem}s in the Google
 * Photos API.
 */
@JsonInclude(Include.NON_NULL)
public class NewMediaItemUpload {

  @JsonProperty("albumId")
  private String albumId;

  @JsonProperty("newMediaItems")
  private List<NewMediaItem> newMediaItems;

  public NewMediaItemUpload(@Nullable String albumId, List<NewMediaItem> newMediaItems) {
    this.albumId = albumId;
    this.newMediaItems = newMediaItems;
  }

  public String getAlbumId() {
    return albumId;
  }

  public List<NewMediaItem> getNewMediaItems() {
    return newMediaItems;
  }
}
