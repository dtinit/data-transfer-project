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
package org.dataportabilityproject.serviceProviders.instagram;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.SecretAuthData;


final class InstagramAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {

  private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

  /**
   * Domain name in the "Callback URL".
   */
  private static final String CALLBACK_PATH = "/callback/instagram";
  private static final String AUTHORIZATION_SERVER_URL =
      "https://api.instagram.com/oauth/authorize";
  private static final String TOKEN_SERVER_URL = "https://api.instagram.com/oauth/access_token";

  private final AppCredentials appCredentials;
  private final List<String> scopes;

  InstagramAuth(AppCredentials appCredentials, List<String> scopes) {
    this.appCredentials = Preconditions.checkNotNull(appCredentials);
    Preconditions.checkArgument(!scopes.isEmpty(), "At least one scope is required.");
    this.scopes = scopes;
  }

  private static InstagramOauthData toAuthData(Credential credential) {
    return InstagramOauthData.create(
        credential.getAccessToken(),
        credential.getRefreshToken(),
        credential.getTokenServerEncodedUrl());
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
        BearerToken.authorizationHeaderAccessMethod(), // Access Method
        getTransport(),
        JSON_FACTORY,
        new GenericUrl("https://api.instagram.com/oauth/access_token"), // GenericUrl
        new ClientParametersAuthentication(appCredentials.key(), appCredentials.secret()),
        appCredentials.key(), // clientId
        "https://api.instagram.com/oauth/authorize/") // encoded authUrl
        .setScopes(ImmutableList.of("basic", "public_content")) // scopes
        .build();

    VerificationCodeReceiver receiver = new LocalServerReceiver.Builder()
        .setHost("localhost").setPort(12345).build();
    try {
      Credential result = new AuthorizationCodeInstalledApp(flow, receiver)
          .authorize("user");
      return SecretAuthData.create(result.getAccessToken());
    } catch (Exception e) {
      throw new IOException("Couldn't authorize", e);
    }
  }

  private synchronized NetHttpTransport getTransport() {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Problem getting token", e);
    }
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
      AuthData initialAuthData,
      String extra) throws IOException {
    Preconditions.checkArgument(Strings.isNullOrEmpty(extra),
        "Extra data not expected for Instagram oauth flow");
    Preconditions.checkArgument(initialAuthData == null,
        "Earlier auth data not expected for Instagram oauth flow");
    AuthorizationCodeFlow flow = createFlow();
    TokenResponse response = flow
        .newTokenRequest(authCode)
        .setRedirectUri(callbackBaseUrl + CALLBACK_PATH) //TODO(chuy): Parameterize
        .execute();
    // Figure out storage
    Credential credential = flow.createAndStoreCredential(response, id);
    return toAuthData(credential);
  }

  /**
   * Creates an AuthorizationCodeFlow for use in online and offline mode.
   */
  private AuthorizationCodeFlow createFlow() throws IOException {
    // set up authorization code flow
    return new AuthorizationCodeFlow.Builder(
        BearerToken.authorizationHeaderAccessMethod(), // Access Method
        InstagramStaticObjects.getHttpTransport(), // HttpTransport
        JSON_FACTORY, // JsonFactory
        new GenericUrl(TOKEN_SERVER_URL), // GenericUrl
        new ClientParametersAuthentication(appCredentials.key(),
            appCredentials.secret()), // HttpExecuteInterceptor
        appCredentials.key(), // clientId
        AUTHORIZATION_SERVER_URL) // encoded authUrl
        .setScopes(scopes) // scopes
        .setDataStoreFactory(InstagramStaticObjects.getDataStoreFactory()).build();
  }
}
