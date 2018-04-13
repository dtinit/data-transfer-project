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

package org.dataportabilityproject.auth.smugmug;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import org.dataportabilityproject.auth.smugmug.SmugMugAuth.AuthInterface;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.model.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugAuthDataGenerator implements AuthDataGenerator {
  private final Logger logger = LoggerFactory.getLogger(SmugMugAuthDataGenerator.class);
  private final AppCredentials appCredentials;
  private final String perms;
  private final AuthInterface authInterface;

  public SmugMugAuthDataGenerator(
      AppCredentials appCredentials, AuthMode authMode, HttpTransport httpTransport) {
    this.appCredentials = appCredentials;
    this.perms = authMode == AuthMode.IMPORT ? "Add" : "Read";
    this.authInterface = new AuthInterface(appCredentials.getKey(), appCredentials.getSecret());
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // Generate a request token and include that as initial auth data
    TokenSecretAuthData authData = null;
    try {
      authData = authInterface.getRequestToken(callbackBaseUrl + "/callback1/smugmug");
    } catch (IOException e) {
      logger.debug("Couldnt get authData {}", e.getMessage());
      return null;
    }

    String url = authInterface.getAuthorizationUrl(authData, perms);
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
        authInterface.getAccessToken((TokenSecretAuthData) initialAuthData, new Verifier(authCode));
    // Note: Some services also require you to validate the accessToken received so another call is made here, Smugmug
    // doesn't look like needs this based on the documentation: https://api.smugmug.com/api/v2/doc/tutorial/authorization.html
    return requestToken;
  }
}
