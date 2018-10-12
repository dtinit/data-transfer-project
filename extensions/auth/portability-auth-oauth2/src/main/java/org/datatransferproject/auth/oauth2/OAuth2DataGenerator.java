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

package org.datatransferproject.auth.oauth2;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol.OAUTH_2;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.util.List;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class OAuth2DataGenerator implements AuthDataGenerator {

  private final List<String> scopes;
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;
  private final String authServerUrl;
  private final String tokenServerUrl;

  OAuth2DataGenerator(OAuth2Config config, AppCredentials appCredentials,
      HttpTransport httpTransport,
      String dataType, AuthMode authMode) {
    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.scopes = authMode == AuthMode.EXPORT
        ? config.getExportScopes().get(dataType)
        : config.getImportScopes().get(dataType);
    this.authServerUrl = config.getAuthUrl();
    this.tokenServerUrl = config.getTokenUrl();
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    String encodedJobId = BaseEncoding.base64Url().encode(id.getBytes(Charsets.UTF_8));
    String url =
        createFlow()
            .newAuthorizationUrl()
            .setRedirectUri(callbackBaseUrl)
            .setState(encodedJobId)
            .build();
    return new AuthFlowConfiguration(url, OAUTH_2, getTokenUrl());
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(
        Strings.isNullOrEmpty(extra), "Extra data not expected for OAuth flow");
    Preconditions.checkArgument(
        initialAuthData == null, "Earlier auth data not expected for OAuth flow");
    AuthorizationCodeFlow flow = createFlow();
    TokenResponse response;
    try {
      response =
          flow.newTokenRequest(authCode)
              .setRedirectUri(callbackBaseUrl)
              .execute();
    } catch (IOException e) {
      throw new RuntimeException("Error calling AuthorizationCodeFlow.execute ", e);
    }

    // Figure out storage
    Credential credential;
    try {
      credential = flow.createAndStoreCredential(response, id);
    } catch (IOException e) {
      throw new RuntimeException(
          "Error calling AuthorizationCodeFlow.createAndStoreCredential ", e);
    }
    return new TokensAndUrlAuthData(
        credential.getAccessToken(),
        credential.getRefreshToken(),
        credential.getTokenServerEncodedUrl());
  }

  private AuthorizationCodeFlow createFlow() {
    AuthorizationCodeFlow.Builder authCodeFlowBuilder = new AuthorizationCodeFlow.Builder(
        BearerToken.authorizationHeaderAccessMethod(), // Access Method
        httpTransport,
        new JacksonFactory(),
        new GenericUrl(tokenServerUrl),
        new ClientParametersAuthentication(
            clientId, clientSecret), // HttpExecuteInterceptor
        clientId, // client ID
        authServerUrl)
        .setScopes(scopes);

    return authCodeFlowBuilder.build();
  }
}
