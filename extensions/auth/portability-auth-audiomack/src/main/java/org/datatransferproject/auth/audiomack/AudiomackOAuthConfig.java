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

package org.datatransferproject.auth.audiomack;

import java.util.Map;
import org.datatransferproject.auth.OAuth1Config;

/**
 * Class that supplies Audiomack-specific OAuth1 info
 * See https://www.audiomack.com/data-api/docs#authenticated-requests
 */
public class AudiomackOAuthConfig implements OAuth1Config {

  @Override
  public String getServiceName() { return "Audiomack"; }

  @Override
  public String getRequestTokenUrl() {
    return "https://api.audiomack.com/v1/request_token";
  }

  @Override
  public String getAuthorizationUrl() {
    return "https://www.audiomack.com/oauth/authenticate";
  }

  @Override
  public String getAccessTokenUrl() {
    return "https://api.audiomack.com/v1/access_token";
  }

  @Override
  public Map<String, String> getExportScopes() {
    return null;
  }

  @Override
  public Map<String, String> getImportScopes() {
    return null;
  }

  @Override
  public String getScopeParameterName() {
    return null;
  }
}
