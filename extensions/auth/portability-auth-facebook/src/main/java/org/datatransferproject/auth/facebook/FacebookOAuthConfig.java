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

package org.datatransferproject.auth.facebook;

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.datatransferproject.auth.OAuth2Config;

import java.util.Map;
import java.util.Set;
import org.datatransferproject.types.common.models.DataVertical;

/**
 * Class that supplies Facebook-specific OAuth2 info
 */
public class FacebookOAuthConfig implements OAuth2Config {
    private static final String AUTHORIZATION_SERVER_URL =
            "https://www.facebook.com/v3.2/dialog/oauth";
    private static final String TOKEN_SERVER_URL =
            "https://graph.facebook.com/v3.2/oauth/access_token";
    private static final String SERVICE_NAME =
            "Facebook";


    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String getAuthUrl() {
        return AUTHORIZATION_SERVER_URL;
    }

    @Override
    public String getTokenUrl() {
        return TOKEN_SERVER_URL;
    }

    @Override
    public Map<DataVertical, Set<String>> getExportScopes() {
        return ImmutableMap.<DataVertical, Set<String>>builder()
            .put(PHOTOS, ImmutableSet.of("user_photos"))
            .put(VIDEOS, ImmutableSet.of("user_videos"))
            .build();
    }

    @Override
    public Map<DataVertical, Set<String>> getImportScopes() {
        return ImmutableMap.<DataVertical, Set<String>>builder()
            .put(PHOTOS, ImmutableSet.of("user_photos"))
            .put(VIDEOS, ImmutableSet.of("user_videos"))
            .build();
    }
}
