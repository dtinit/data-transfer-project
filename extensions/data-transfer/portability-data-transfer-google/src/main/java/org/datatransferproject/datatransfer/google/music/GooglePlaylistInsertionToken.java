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

package org.datatransferproject.datatransfer.google.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.DataModel;

/**
 * Class representing a Google Playlist Token (short-lived) which is from the response of
 * UpdatePlaylist API and stored in each job's memory (only used in @{GoogleMusicImporter}). This
 * token is used by Google Music public API to search the playlist easily when inserting playlist
 * items. This token should be unique within the upstream system from which the data originated. No
 * particular format is guaranteed.
 */
@JsonTypeName("org.dataportability:GooglePlaylistInsertionToken")
public class GooglePlaylistInsertionToken extends DataModel {
  private final String token;

  @JsonCreator
  public GooglePlaylistInsertionToken(@JsonProperty("token") String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
