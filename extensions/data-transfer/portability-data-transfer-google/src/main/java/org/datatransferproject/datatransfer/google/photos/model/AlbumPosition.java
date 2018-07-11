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

package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlbumPosition {

  @JsonProperty("position")
  private PositionType position;

  @JsonProperty("relativeMediaItemId")
  private String relativeMediaItemId;

  @JsonProperty("relativeEnrichmentItemId")
  private String relativeEnrichmentItemId;

  public enum PositionType {
    POSITION_TYPE_UNSPECIFIED,
    FIRST_IN_ALBUM,
    LAST_IN_ALBUM,
    AFTER_MEDIA_ITEM,
    AFTER_ENRICHMENT_ITEM;
  }
}
