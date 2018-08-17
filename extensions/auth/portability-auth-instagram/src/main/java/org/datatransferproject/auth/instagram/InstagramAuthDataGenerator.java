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
package org.datatransferproject.auth.instagram;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.List;

import static org.datatransferproject.spi.api.types.AuthFlowConfiguration.AuthProtocol;
import static org.datatransferproject.spi.api.types.AuthFlowConfiguration.AuthProtocol.OAUTH_2;

public class InstagramAuthDataGenerator implements AuthDataGenerator {
  private static final AuthProtocol AUTHORIZATION_PROTOCOL = OAUTH_2;
  private static final String CALLBACK_PATH = "/callback/instagram";
  private static final String AUTHORIZATION_SERVER_URL =
      "https://api.instagram.com/oauth/authorize";
  private static final String TOKEN_SERVER_URL = "https://api.instagram.com/oauth/access_token";
  private final AppCredentials appCredentials;
  private final HttpTransport httpTransport;
  private final List<String> scopes = ImmutableList.of("basic");

  public InstagramAuthDataGenerator(AppCredentials appCredentials, HttpTransport httpTransport) {
    this.appCredentials = appCredentials;
    this.httpTransport = httpTransport;
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // TODO: move to common location
    String encodedJobId = BaseEncoding.base64Url().encode(id.getBytes(Charsets.UTF_8));
    String url =
        createFlow()
            .newAuthorizationUrl()
            .setRedirectUri(callbackBaseUrl + CALLBACK_PATH)
            .setState(encodedJobId)
            .build();
    return new AuthFlowConfiguration(url, AUTHORIZATION_PROTOCOL);
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(
        Strings.isNullOrEmpty(extra), "Extra data not expected for Instagram oauth flow");
    Preconditions.checkArgument(
        initialAuthData == null, "Earlier auth data not expected for Instagram oauth flow");
    AuthorizationCodeFlow flow = createFlow();
    TokenResponse response = null;
    try {
      response =
          flow.newTokenRequest(authCode)
              .setRedirectUri(callbackBaseUrl + CALLBACK_PATH) // TODO(chuy): Parameterize
              .execute();
    } catch (IOException e) {
      throw new RuntimeException("Error calling AuthorizationCodeFlow.execute ", e);
    }

    // Figure out storage
    Credential credential = null;
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
    return new AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(), // Access Method
            httpTransport,
            new JacksonFactory(),
            new GenericUrl(TOKEN_SERVER_URL),
            new ClientParametersAuthentication(
                appCredentials.getKey(), appCredentials.getSecret()), // HttpExecuteInterecptor
            appCredentials.getKey(), // client ID
            AUTHORIZATION_SERVER_URL)
        .setScopes(scopes)
        // TODO: needed for local caching
        // .setDataStoreFactory(InstagramStaticObjects.getDataStoreFactory())
        .build();
  }
}
