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

package org.datatransferproject.auth.spotify;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;

/**
 * Class that supplies Spotify-specific OAuth2 info
 */
public class SpotifyOAuthConfig implements OAuth2Config {

  // https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow

  @Override
  public String getServiceName() {
    return "Spotify";
  }

  @Override
  public String getAuthUrl() {
    return "https://accounts.spotify.com/authorize";
  }

  @Override
  public String getTokenUrl() {
    return "https://accounts.spotify.com/api/token";
  }

  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PLAYLISTS", ImmutableSet.of("playlist-read-private"))
        .build();
  }

  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PLAYLISTS", ImmutableSet.of("playlist-modify-private"))
        .build();
  }

  @Override
  public Map<String, String> getAdditionalAuthUrlParameters() {
    return ImmutableMap.of("show_dialog", "true");
  }
}
