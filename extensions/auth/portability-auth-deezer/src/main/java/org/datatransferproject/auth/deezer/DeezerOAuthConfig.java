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

package org.datatransferproject.auth.deezer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.auth.OAuth2TokenResponse;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Class that supplies Dezzer-specific OAuth2 info
 */
public class DeezerOAuthConfig implements OAuth2Config {
  private static Pattern AUTH_TOKEN_PATTERN = Pattern.compile("access_token=(\\w+)&expires=0");

  // https://developers.deezer.com/api/oauth

  @Override
  public String getServiceName() {
    return "Deezer";
  }

  @Override
  public String getAuthUrl() {
    return "https://connect.deezer.com/oauth/auth.php";
  }

  @Override
  public String getTokenUrl() {
    return "https://connect.deezer.com/oauth/access_token.php";
  }


  // For descriptions of scopes see: https://developers.deezer.com/api/permissions
  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PLAYLISTS", ImmutableSet.of("offline_access,manage_library"))
        .build();
  }

  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PLAYLISTS", ImmutableSet.of("offline_access,manage_library"))
        .build();
  }

  @Override
  public Map<String, String> getAdditionalAuthUrlParameters() {
    return ImmutableMap.of();
  }

  @Override
  public TokensAndUrlAuthData getResponseClass(String result) throws IOException {
    Matcher matcher = AUTH_TOKEN_PATTERN.matcher(result);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(result + " didn't match expected regex: "
          + AUTH_TOKEN_PATTERN.pattern());
    }

    return new TokensAndUrlAuthData(
        matcher.group(1),
        null,
        getTokenUrl());
  }

}
