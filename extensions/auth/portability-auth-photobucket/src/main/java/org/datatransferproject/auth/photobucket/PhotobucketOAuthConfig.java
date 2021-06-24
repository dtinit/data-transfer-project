/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.auth.photobucket;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class PhotobucketOAuthConfig implements OAuth2Config {
  private static final String PB_SERVICE_ID = "Photobucket";
  private static final Pattern AUTH_TOKEN_PATTERN =
      Pattern.compile(".*\"access_token\":\"([\\w.]+)*\".*");

  @Override
  public String getServiceName() {
    return PB_SERVICE_ID;
  }

  @Override
  public String getAuthUrl() {
    return "https://auth.stage.photobucket.com/oauth/authorize";
  }

  @Override
  public String getTokenUrl() {
    return "https://auth.stage.photobucket.com/oauth/token";
  }

  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PHOTOS", ImmutableSet.of("create.media"))
        .put("VIDEOS", ImmutableSet.of("create.media"))
        .build();
  }

  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("PHOTOS", ImmutableSet.of("create.media"))
        .put("VIDEOS", ImmutableSet.of("create.media"))
        .build();
  }

  @Override
  public TokensAndUrlAuthData getResponseClass(String result) throws IllegalArgumentException {
    Matcher matcher = AUTH_TOKEN_PATTERN.matcher(result);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          result + " didn't match expected regex: " + AUTH_TOKEN_PATTERN.pattern());
    }

    return new TokensAndUrlAuthData(matcher.group(1), null, getTokenUrl());
  }
}
