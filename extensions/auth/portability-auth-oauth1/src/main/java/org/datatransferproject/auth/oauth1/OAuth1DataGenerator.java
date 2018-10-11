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

package org.datatransferproject.auth.oauth1;

import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class OAuth1DataGenerator implements AuthDataGenerator {

  private final List<String> scopes;
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;
  private final String authorizeUrl;
  private final String requestTokenUrl;
  private final String accessTokenUrl;

  OAuth1DataGenerator(OAuth1Config config, AppCredentials appCredentials,
      HttpTransport httpTransport,
      String dataType, AuthMode mode) {
    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.scopes = mode == AuthMode.EXPORT
        ? config.getExportScopes().get(dataType)
        : config.getImportScopes().get(dataType);
    this.authorizeUrl = config.getAuthorizeUrl();
    this.requestTokenUrl = config.getRequestTokenUrl();
    this.accessTokenUrl = config.getAccessTokenUrl();
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    return null;
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    Preconditions.checkNotNull(initialAuthData, "Earlier auth data expected for OAuth1 flow");

    OAuthGetAccessToken getAccessToken = new OAuthGetAccessToken(accessTokenUrl);
    getAccessToken.temporaryToken = ((TokenSecretAuthData) initialAuthData).getToken();
    getAccessToken.transport = httpTransport;
    getAccessToken.verifier = authCode;

    TokenSecretAuthData accessToken = null;
    try {
      OAuthCredentialsResponse token = getAccessToken.execute();
      accessToken = new TokenSecretAuthData(token.token, token.tokenSecret);
    } catch (IOException e) {
      // TODO: what to do here?
      e.printStackTrace();
    }

    return accessToken;
  }
}
