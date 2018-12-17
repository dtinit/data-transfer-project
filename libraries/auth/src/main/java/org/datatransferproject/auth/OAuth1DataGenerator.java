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

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.auth.OAuth1Config.OAuth1Step;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.common.PortabilityCommon.AuthProtocol;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

import java.io.IOException;

/** General implementation of an {@link AuthDataGenerator} for OAuth1. */
public class OAuth1DataGenerator implements AuthDataGenerator {

  private static final String OUT_OF_BOUNDS_CALLBACK = "oob"; // TODO: is this universal?

  private final OAuth1Config config;
  private final Monitor monitor;
  private final String scope;
  // TODO: handle dynamic updates of client ids and secrets #597
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;

  OAuth1DataGenerator(
      OAuth1Config config,
      AppCredentials appCredentials,
      HttpTransport httpTransport,
      String datatype,
      AuthMode mode,
      Monitor monitor) {
    this.config = config;
    this.monitor = monitor;
    validateConfig();

    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.scope =
        mode == AuthMode.EXPORT
            ? config.getExportScopes().get(datatype)
            : config.getImportScopes().get(datatype);
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    String callback =
        (Strings.isNullOrEmpty(callbackBaseUrl)) ? OUT_OF_BOUNDS_CALLBACK : callbackBaseUrl;
    OAuthGetTemporaryToken tempTokenRequest =
        new OAuthGetTemporaryToken(config.getRequestTokenUrl());
    tempTokenRequest.callback = callback;
    tempTokenRequest.transport = httpTransport;
    tempTokenRequest.consumerKey = clientId;
    tempTokenRequest.signer = config.getRequestTokenSigner(clientSecret);
    if (config.whenAddScopes() == OAuth1Step.REQUEST_TOKEN) {
      tempTokenRequest.set(config.getScopeParameterName(), scope);
    }
    TokenSecretAuthData authData;
    try {
      // get request token
      OAuthCredentialsResponse tempTokenResponse = tempTokenRequest.execute();
      authData = new TokenSecretAuthData(tempTokenResponse.token, tempTokenResponse.tokenSecret);
    } catch (IOException e) {
      monitor.severe(() -> "Error retrieving request token", e);
      return null;
    }

    OAuthAuthorizeTemporaryTokenUrl authorizeUrl =
        new OAuthAuthorizeTemporaryTokenUrl(config.getAuthorizationUrl());
    authorizeUrl.temporaryToken = authData.getToken();
    if (config.whenAddScopes() == OAuth1Step.AUTHORIZATION) {
      authorizeUrl.set(config.getScopeParameterName(), scope);
    }
    String url = authorizeUrl.build();

    return new AuthFlowConfiguration(url, getTokenUrl(), AuthProtocol.OAUTH_1, authData);
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(
        Strings.isNullOrEmpty(extra), "Extra data not expected for OAuth flow");
    Preconditions.checkArgument(
        initialAuthData != null, "Initial auth data expected for " + config.getServiceName());

    OAuthGetAccessToken accessTokenRequest = new OAuthGetAccessToken(config.getAccessTokenUrl());
    accessTokenRequest.transport = httpTransport;
    accessTokenRequest.temporaryToken = ((TokenSecretAuthData) initialAuthData).getToken();
    accessTokenRequest.consumerKey = clientId;
    accessTokenRequest.verifier = authCode;
    accessTokenRequest.signer =
        config.getAccessTokenSigner(
            clientSecret, ((TokenSecretAuthData) initialAuthData).getSecret());
    TokenSecretAuthData accessToken;
    try {
      OAuthCredentialsResponse response = accessTokenRequest.execute();
      accessToken = new TokenSecretAuthData(response.token, response.tokenSecret);
    } catch (IOException e) {
      monitor.severe(() -> "Error retrieving request token", e);
      return null;
    }

    return accessToken;
  }

  private void validateConfig() {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(config.getServiceName()), "Config is missing service name");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(config.getRequestTokenUrl()), "Config is missing request token url");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(config.getAuthorizationUrl()),
        "Config is missing authorization url");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(config.getAccessTokenUrl()), "Config is missing access token url");

    // This decision is not OAuth spec, but part of an effort to prevent accidental scope omission
    Preconditions.checkArgument(
        config.getExportScopes() != null, "Config is missing export scopes");
    Preconditions.checkArgument(
        config.getImportScopes() != null, "Config is missing import scopes");
  }
}
