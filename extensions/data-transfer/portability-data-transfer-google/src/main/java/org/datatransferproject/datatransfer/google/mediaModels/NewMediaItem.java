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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datatransferproject.types.common.models.photos.PhotoModel;

/**
 * Class containing all the information uploaded when creating a new media item through the Google
 * Photos API.  Represents an intermediate step between a {@link PhotoModel}
 * and a {@link GoogleMediaItem} (which is created in response to a {@link NewMediaItemUpload}).
 * Note that this does not contain the content of the media item (photo or video)
 * itself, just the description and a {@code SimpleMediaItem} containing the upload token
 * corresponding to the previously-uploaded content.
 */
public class NewMediaItem {
  @JsonProperty("description")
  private String description;

  @JsonProperty("simpleMediaItem")
  private SimpleMediaItem simpleMediaItem;

  @JsonCreator
  public NewMediaItem(String description, SimpleMediaItem simpleMediaItem) {
    this.description = description;
    this.simpleMediaItem = simpleMediaItem;
  }

  public NewMediaItem(String description, String uploadToken) {
    this.description = description;
    this.simpleMediaItem = new SimpleMediaItem(uploadToken);
  }

  public SimpleMediaItem getSimpleMediaItem() {
    return simpleMediaItem;
  }

  public String getDescription() {
    return description;
  }

}
