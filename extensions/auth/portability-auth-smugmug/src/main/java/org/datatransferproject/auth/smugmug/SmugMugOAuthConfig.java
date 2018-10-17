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

package org.datatransferproject.auth.smugmug;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthSigner;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.datatransferproject.auth.OAuth1Config;

/**
 * Class that supplies SmugMug-specific OAuth1 info
 * See https://smugmug.atlassian.net/wiki/spaces/API/pages/689052/OAuth
 */
public class SmugMugOAuthConfig implements OAuth1Config {

  @Override
  public String getServiceName() {
    return "SmugMug";
  }

  @Override
  public String getRequestTokenUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getRequestToken";
  }

  @Override
  public String getAuthorizationUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/authorize";
  }

  @Override
  public String getAccessTokenUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getAccessToken";
  }

  @Override
  public Map<String, String> getExportScopes() {
    return ImmutableMap.of("PHOTOS", "Read");
  }

  @Override
  public Map<String, String> getImportScopes() {
    return ImmutableMap.of("PHOTOS", "Add");
  }

  @Override
  public String getScopeParameterName() {
    return "Permissions";
  }

  @Override
  public OAuthSigner getRequestTokenSigner(String clientSecret) {
    OAuthHmacSigner signer = new OAuthHmacSigner();
    signer.clientSharedSecret = clientSecret;
    return signer;
  }

  @Override
  public OAuthSigner getAccessTokenSigner(String clientSecret, String tokenSecret) {
    OAuthHmacSigner signer = new OAuthHmacSigner();
    signer.clientSharedSecret = clientSecret;
    signer.tokenSharedSecret = tokenSecret;
    return signer;
  }
}
