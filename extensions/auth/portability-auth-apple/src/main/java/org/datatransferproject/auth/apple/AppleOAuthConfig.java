/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.auth.apple;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

import java.util.Map;
import java.util.Set;

import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.MUSIC;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

/**
 *  OAuth Configuration for Apple Services.
 */
public class AppleOAuthConfig implements OAuth2Config {
    private static final String PHOTOS_APPEND_ONLY = "photos-appendonly";
    private static final String VIDEOS_APPEND_ONLY = "videos-appendonly";
    private static final String PHOTOS_READONLY_ONLY = "photos-readonly";
    private static final String VIDEOS_READONLY_ONLY = "videos-readonly";
    private static final String MUSIC_PLAYLISTS_APPEND_ONLY = "music-playlists-appendonly";
    private static final String MUSIC_PLAYLISTS_READ_ONLY = "music-playlists-readonly";

    @Override
    public String getServiceName() {
        return "Apple";
    }

    @Override
    public String getAuthUrl() {
        return "https://appleid.apple.com/auth/oauth2/v2/authorize";
    }

    @Override
    public String getTokenUrl() {
        return "https://appleid.apple.com/auth/oauth2/v2/token";
    }

    @Override
    public Map<DataVertical, Set<String>> getExportScopes() {
      return ImmutableMap.<DataVertical, Set<String>>builder()
              .put(PHOTOS, ImmutableSet.of(PHOTOS_READONLY_ONLY))
              .put(VIDEOS, ImmutableSet.of(VIDEOS_READONLY_ONLY))
              .put(MEDIA, ImmutableSet.of(PHOTOS_READONLY_ONLY, VIDEOS_READONLY_ONLY))
              .put(MUSIC, ImmutableSet.of(MUSIC_PLAYLISTS_READ_ONLY))
        .build();
    }

    @Override
    public Map<DataVertical, Set<String>> getImportScopes() {
        return ImmutableMap.<DataVertical, Set<String>>builder()
                .put(PHOTOS, ImmutableSet.of(PHOTOS_APPEND_ONLY))
                .put(VIDEOS, ImmutableSet.of(VIDEOS_APPEND_ONLY))
                .put(MEDIA, ImmutableSet.of(PHOTOS_APPEND_ONLY, VIDEOS_APPEND_ONLY))
                .put(MUSIC, ImmutableSet.of(MUSIC_PLAYLISTS_APPEND_ONLY))
                .build();
    }
}
