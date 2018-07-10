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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.auth.smugmug.SmugMugAuth.SmugMugOauthInterface;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.model.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/* SmugmugAuthDataGenerator used for obtaining auth credentials for the Smugmug API*/
public class SmugMugAuthDataGenerator implements AuthDataGenerator {
  private final Logger logger = LoggerFactory.getLogger(SmugMugAuthDataGenerator.class);
  private final String perms;
  private final SmugMugOauthInterface smugMugOauthInterface;

  public SmugMugAuthDataGenerator(AppCredentials appCredentials, AuthMode authMode) {
    this.perms = authMode == AuthMode.IMPORT ? "Add" : "Read";
    this.smugMugOauthInterface =
        new SmugMugOauthInterface(appCredentials.getKey(), appCredentials.getSecret());
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // Generate a request token and include that as initial auth data
    TokenSecretAuthData authData = null;
    try {
      authData = smugMugOauthInterface.getRequestToken(callbackBaseUrl + "/callback1/smugmug");
    } catch (IOException e) {
      logger.warn("Couldnt get authData {}", e.getMessage());
      return null;
    }

    String url = smugMugOauthInterface.getAuthorizationUrl(authData, perms);
    return new AuthFlowConfiguration(url, authData);
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {

    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    Preconditions.checkNotNull(
        initialAuthData,
        "Earlier auth data expected for Smugmug flow"); // Turn initial auth data into a Token
    // get an access token from the token in inital auth data, verified with the authcode
    TokenSecretAuthData requestToken =
        smugMugOauthInterface.getAccessToken(
            (TokenSecretAuthData) initialAuthData, new Verifier(authCode));
    // Note: Some services also require you to validate the accessToken received so another call is
    // made here, Smugmug
    // doesn't look like needs this based on the documentation:
    // https://api.smugmug.com/api/v2/doc/tutorial/authorization.html
    return requestToken;
  }
}
