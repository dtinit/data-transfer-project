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

package org.datatransferproject.auth;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthSigner;
import java.util.Map;

/**
 * Interface for providing information necessary to run OAuth1 flow
 */
public interface OAuth1Config {

  /**
   * Returns the name of the service, used for display, and client id and secret retrieval
   */
  String getServiceName();

  /**
   * Returns the request token url
   */
  String getRequestTokenUrl();

  /**
   * Returns the authorization url
   */
  String getAuthorizationUrl();

  /**
   * Returns the access token url
   */
  String getAccessTokenUrl();

  /**
   * Returns a map of scopes needed for export, keyed by data type (e.g., PHOTOS, CALENDAR) as
   * defined in the auth data generator or elsewhere
   * Note: this method assumes that scopes are required to use this service
   */
  Map<String, String> getExportScopes();

  /**
   * Returns a map of scopes needed for import, keyed by data type (e.g., PHOTOS, CALENDAR) as
   * defined in the auth data generator or elsewhere
   * Note: this method assumes that scopes are required to use this service
   */
  Map<String, String> getImportScopes();

  /**
   * Returns the parameter name for scopes
   * Note: this method assumes that scopes are required to use this service
   */
  String getScopeParameterName();

  /**
   * Shows what step the scopes should be requested in
   * Note: this method assumes that scopes are required to use this service
   */
  default OAuth1Step whenAddScopes() {
    return OAuth1Step.AUTHORIZATION;
  }

  /**
   * Returns the {@link OAuthSigner} for the initial token request
   */
  default OAuthSigner getRequestTokenSigner(String clientSecret) {
    OAuthHmacSigner signer = new OAuthHmacSigner();
    signer.clientSharedSecret = clientSecret;
    return signer;
  }

  /**
   * Returns the {@link OAuthSigner} for the access token request
   */
  default OAuthSigner getAccessTokenSigner(String clientSecret, String tokenSecret) {
    OAuthHmacSigner signer = new OAuthHmacSigner();
    signer.clientSharedSecret = clientSecret;
    signer.tokenSharedSecret = tokenSecret;
    return signer;
  }

  enum OAuth1Step {
    REQUEST_TOKEN,
    AUTHORIZATION,
    ACCESS_TOKEN;
  }
}
