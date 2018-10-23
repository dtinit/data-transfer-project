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

package org.datatransferproject.auth.flickr;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthSigner;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.datatransferproject.auth.OAuth1Config;

/**
 * Class that supplies Flickr-specific OAuth1 info
 * See https://www.flickr.com/services/api/auth.oauth.html
 */
public class FlickrOAuthConfig implements OAuth1Config {

  @Override
  public String getServiceName() {
    return "Flickr";
  }

  @Override
  public String getRequestTokenUrl() {
    return "https://www.flickr.com/services/oauth/request_token";
  }

  @Override
  public String getAuthorizationUrl() {
    return "https://www.flickr.com/services/oauth/authorize";
  }

  @Override
  public String getAccessTokenUrl() {
    return "https://www.flickr.com/services/oauth/access_token";
  }

  @Override
  public Map<String, String> getExportScopes() {
    return ImmutableMap.of("PHOTOS", "read");
  }

  @Override
  public Map<String, String> getImportScopes() {
    return ImmutableMap.of("PHOTOS", "write");
  }

  @Override
  public String getScopeParameterName() {
    return "perms";
  }
}
