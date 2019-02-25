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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Interface for providing information necessary to run OAuth2 flow
 */
public interface OAuth2Config {

  /**
  Returns the name of the service, used for display, and client id and secret retrieval
   */
  String getServiceName();

  /**
   * Returns the authorization URL to be used
   */
  String getAuthUrl();

  /**
   * Returns the token URL to be used
   */
  String getTokenUrl();

  /**
   * Returns a map of scopes needed for export, keyed by data type (e.g., PHOTOS, CALENDAR) as
   * defined in the auth data generator or elsewhere
   */
  Map<String, Set<String>> getExportScopes();

  /**
   * Returns a map of scopes needed for import, keyed by data type (e.g., PHOTOS, CALENDAR) as
   * defined in the auth data generator or elsewhere
   */
  Map<String, Set<String>> getImportScopes();

  /**
   * Returns a map of any additional parameters necessary for this service
   */
  default Map<String, String> getAdditionalAuthUrlParameters() {
    return null;
  }

  /**
   * Returns the class that can deserialize the OAuth response.
   */
  default TokensAndUrlAuthData getResponseClass(String result) throws IOException {
    OAuth2TokenResponse response = new ObjectMapper().readValue(result, OAuth2TokenResponse.class);

    return new TokensAndUrlAuthData(
        response.getAccessToken(),
        response.getRefreshToken(),
        getTokenUrl());
  }

}
