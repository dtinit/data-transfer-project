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

package org.datatransferproject.auth.imgur;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;

/**
 * Class that provides Imgur-specific information for OAuth2
 * See https://apidocs.imgur.com/#authorization-and-oauth
 */
public class ImgurOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Imgur";
  }

  @Override
  public String getAuthUrl() {
    return "https://api.imgur.com/oauth2/authorize";
  }

  @Override
  public String getTokenUrl() {
    return "https://api.imgur.com/oauth2/token";
  }

  // Imgur doesn't require scopes
  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.of("PHOTOS", ImmutableSet.of(""));
  }

  @Override
  public Map<String, Set<String>> getImportScopes() {
    // Imgur importer is not implemented yet
    return ImmutableMap.of();
  }
}
