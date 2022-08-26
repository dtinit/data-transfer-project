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

package org.datatransferproject.auth.instagram;

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

/**
 * Class that provides Instagram-specific information for OAuth2
 */
public class InstagramOAuthConfig implements OAuth2Config {

  // https://www.instagram.com/developer/authentication/

  @Override
  public String getServiceName() {
    return "Instagram";
  }

  @Override
  public String getAuthUrl() {
    return "https://api.instagram.com/oauth/authorize";
  }

  @Override
  public String getTokenUrl() {
    return "https://api.instagram.com/oauth/access_token";
  }

  // See https://www.instagram.com/developer/authorization/
  @Override
  public Map<DataVertical, Set<String>> getExportScopes() {
    return ImmutableMap.of(PHOTOS, ImmutableSet.of("basic"));
  }

  // Instagram does not provide an API for import; https://help.instagram.com/442418472487929
  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.of();
  }
}
