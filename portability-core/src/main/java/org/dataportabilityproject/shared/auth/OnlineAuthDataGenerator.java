/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.shared.auth;

import java.io.IOException;
import java.util.UUID;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/** Methods to generate AuthData for online */
public interface OnlineAuthDataGenerator {
  /**
   * Provide a authUrl to redirect the user to authenticate. In the Oauth2 case, this is the
   * authorization code authUrl.
   *
   * @param callbackBaseUrl the url to the api server serving the callback for the auth request
   * @param jobId the ID of the PortabilityJob
   */
  AuthFlowInitiator generateAuthUrl(String callbackBaseUrl, UUID jobId) throws IOException;

  /**
   * Generate auth data given the code, identifier, and, optional, initial auth data that was used
   * for earlier steps of the authentication flow.
   *
   * @param callbackBaseUrl the url to the api server serving the callback for the auth request
   * @param authCode The authorization code or oauth verififer after user authorization
   * @param jobId the ID of the PortabilityJob
   * @param initialAuthData optional data resulting from the initial auth step
   * @param extra optional additional code, password, etc.
   */
  AuthData generateAuthData(
      String callbackBaseUrl, String authCode, UUID jobId, AuthData initialAuthData, String extra)
      throws IOException;
}
