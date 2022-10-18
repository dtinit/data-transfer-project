/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class containing the individual response from creating {@code GooglePlaylistItem} to the Google
 * Music API.
 */
public class NewPlaylistItemResult {

  @JsonProperty("status")
  private Status status;

  @JsonProperty("playlistItem")
  private GooglePlaylistItem playlistItem;

  public Status getStatus() {
    return status;
  }

  public GooglePlaylistItem getPlaylistItem() {
    return playlistItem;
  }
}
