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

package org.datatransferproject.auth.twitter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.datatransferproject.auth.oauth2.OAuth2Config;

public class TwitterOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Twitter";
  }

  @Override
  public String getAuthUrl() {
    return "https://api.twitter.com/oauth/authenticate";
  }

  @Override
  public String getTokenUrl() {
    return "https://api.twitter.com/oauth2/token";
  }

  @Override
  public Map<String, List<String>> getExportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("PHOTOS", ImmutableList.of("read"))
        .build();
  }

  @Override
  public Map<String, List<String>> getImportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("PHOTOS", ImmutableList.of("write"))
        .build();
  }
}
