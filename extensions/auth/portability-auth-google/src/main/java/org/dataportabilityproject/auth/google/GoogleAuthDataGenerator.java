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
package org.dataportabilityproject.auth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
/**
 * Provides configuration for conducting an OAuth flow against the Google AD API. Returned tokens
 * can be used to make requests against the Google Graph API.
 *
 * <p>The flow is a two-step process. First, the user is sent to an authorization page and is then
 * redirected to a address in this system with the authorization code. The second step takes the
 * authorization code and posts it against the AD API to obtain a token for querying the Graph API.
 */
public class GoogleAuthDataGenerator implements AuthDataGenerator {
  // TODO: Reduce requested scopes by service and authorization mode (readwrite/read)
  private static final ImmutableCollection<String> SCOPES =
      ImmutableSet.of(CalendarScopes.CALENDAR, PeopleServiceScopes.CONTACTS, TasksScopes.TASKS);

  private final String redirectPath;
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;
  private final ObjectMapper objectMapper;

  /**
   * @param redirectPath the path part this generator is configured to request OAuth authentication
   *     code responses be sent to
   * @param clientId The Application ID that the registration portal (apps.dev.microsoft.com)
   *     assigned the portability instance
   * @param clientSecret The application secret that was created in the app registration portal for
   *     the portability instance
   * @param httpTransport The http transport to use for underlying GoogleAuthorizationCodeFlow
   * @param objectMapper The json factory provider
   */
  public GoogleAuthDataGenerator(
      String redirectPath,
      String clientId,
      String clientSecret,
      HttpTransport httpTransport,
      ObjectMapper objectMapper) {

    this.redirectPath = redirectPath;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.httpTransport = httpTransport;
    this.objectMapper = objectMapper;
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // TODO: Move to common location
    String encodedJobId = BaseEncoding.base64Url().encode(id.getBytes(Charsets.UTF_8));
    String url =
        createFlow()
            .newAuthorizationUrl()
            .setRedirectUri(callbackBaseUrl + redirectPath)
            .setState(encodedJobId)
            .build();
    return new AuthFlowConfiguration(url);
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    Preconditions.checkState(
        initialAuthData == null, "Earlier auth data not expected for Google flow");
    AuthorizationCodeFlow flow;
    TokenResponse response;
    try {
      flow = createFlow();
      response =
          flow.newTokenRequest(authCode)
              .setRedirectUri(callbackBaseUrl + redirectPath) // TODO(chuy): Parameterize
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
    // TODO: Extract the Google User ID from the ID token in the auth response
    // GoogleIdToken.Payload payload = ((GoogleTokenResponse) response).parseIdToken().getPayload();
    return new TokensAndUrlAuthData(
        credential.getAccessToken(),
        credential.getRefreshToken(),
        credential.getTokenServerEncodedUrl());
  }

  /** Creates an AuthorizationCodeFlow for use in online and offline mode. */
  private GoogleAuthorizationCodeFlow createFlow() {
    return new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            // Figure out how to adapt objectMapper.getFactory(),
            new JacksonFactory(),
            clientId,
            clientSecret,
            SCOPES)
        .setAccessType("offline")
        // TODO: Needed for local caching
        // .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
        .setApprovalPrompt("force")
        .build();
  }
}
