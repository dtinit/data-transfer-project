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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.types.common.models.DataVertical;

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
   * Returns a list of export data types (e.g., PHOTOS, CALENDAR) this config is designed to support.
   * @return
   */
  List<DataVertical> getExportTypes();

  /**
   * Returns a list of import data types (e.g., PHOTOS, CALENDAR) this config is designed to support.
   * @return
   */
  List<DataVertical> getImportTypes();

  /**
   * Return a map of parameters that will be added to the OAuth request.
   *
   * <p>The OAuth 1 spec allows service-defined parameters on the request token and authorization
   * URLs. Typically this is used for token scope, but may have additional uses as well.
   *
   * <p>Some services require different parameters (scopes) for different data types. The minimum
   * privilege for the given mode (EXPORT, IMPORT) should be used.
   */
  default Map<String, String> getAdditionalUrlParameters(
      DataVertical dataType, AuthMode mode, OAuth1Step step) {
    return Collections.emptyMap();
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
