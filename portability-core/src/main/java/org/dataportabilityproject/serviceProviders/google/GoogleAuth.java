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
package org.dataportabilityproject.serviceProviders.google;

import static com.google.common.base.Preconditions.checkArgument;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * A generator of Google {@link Credential}
 */
class GoogleAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {

  /**
   * Port in the "Callback URL".
   */
  private static final int PORT = 8080;
  private static final String CALLBACK_PATH = "/callback/google";

  /**
   * Domain name in the "Callback URL".
   */
  private static final String DOMAIN = "127.0.0.1";

  private final AppCredentials appCredentials;
  private final List<String> scopes;

  GoogleAuth(AppCredentials appCredentials, List<String> scopes) {
    this.appCredentials = Preconditions.checkNotNull(appCredentials);
    Preconditions.checkArgument(!scopes.isEmpty(), "At least one scope is required.");
    this.scopes = scopes;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    Credential c = authorize();
    return toAuthData(c);
  }

  @Override
  public AuthFlowInitiator generateAuthUrl(String callbackBaseUrl, String id) throws IOException {
    String url = createFlow()
        .newAuthorizationUrl()
        .setRedirectUri(callbackBaseUrl + CALLBACK_PATH)
        .setState(id) // TODO: Encrypt
        .build();
    return AuthFlowInitiator.create(url);
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      @Nullable AuthData initialAuthData, @Nullable String extra) throws IOException {
    Preconditions.checkState(initialAuthData == null,
        "Earlier auth data not expected for Google flow");
    AuthorizationCodeFlow flow = createFlow();
    TokenResponse response = flow
        .newTokenRequest(authCode)
        .setRedirectUri(callbackBaseUrl + CALLBACK_PATH) //TODO(chuy): Parameterize
        .execute();
    // Figure out storage
    Credential credential = flow.createAndStoreCredential(response, id);
    // Extract the Google User ID from the ID token in the auth response
    // GoogleIdToken.Payload payload = ((GoogleTokenResponse) response).parseIdToken().getPayload();
    return toAuthData(credential);
  }


  Credential getCredential(AuthData authData) {
    checkArgument(authData instanceof GoogleTokenData,
        "authData expected to be TokenSecretAuthData not %s",
        authData.getClass().getCanonicalName());
    GoogleTokenData tokenData = (GoogleTokenData) authData;

    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
        .setTransport(GoogleStaticObjects.getHttpTransport())
        .setJsonFactory(JSON_FACTORY)
        .setClientAuthentication(new ClientParametersAuthentication(appCredentials.key(),
            appCredentials.secret()))
        .setTokenServerEncodedUrl(tokenData.tokenServerEncodedUrl())
        .build()
        .setAccessToken(tokenData.accessToken())
        .setRefreshToken(tokenData.refreshToken())
        .setExpiresInSeconds(0L);
  }

  private AuthData toAuthData(Credential credential) {
    return GoogleTokenData.create(
        credential.getAccessToken(),
        credential.getRefreshToken(),
        credential.getTokenServerEncodedUrl());
  }

  private Credential authorize() throws IOException {
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow = createFlow();

    // authorize
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
        .setHost(DOMAIN)
        .setPort(PORT)
        .build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  /**
   * Creates an AuthorizationCodeFlow for use in online and offline mode.
   */
  private GoogleAuthorizationCodeFlow createFlow()
      throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(
        GoogleStaticObjects.getHttpTransport(),
        JSON_FACTORY,
        appCredentials.key(),
        appCredentials.secret(),
        scopes)
        .setAccessType("offline")
        .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
        .setApprovalPrompt("force")
        .build();
  }
}
