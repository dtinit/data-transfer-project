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
package org.datatransferproject.auth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
/**
 * Provides configuration for conducting an OAuth flow against the Google AD API. Returned tokens
 * can be used to make requests against the Google Graph API.
 *
 * <p>The flow is a two-step process. First, the user is sent to an authorization page and is then
 * redirected to a address in this system with the authorization code. The second step takes the
 * authorization code and posts it against the AD API to obtain a token for querying the Graph API.
 */
public class GoogleAuthDataGenerator implements AuthDataGenerator {
  // The scopes necessary to import each supported data type.
  // These are READ/WRITE scopes
  private static final Map<String, List<String>> IMPORT_SCOPES =
      ImmutableMap.<String, List<String>>builder()
          .put("calendar", ImmutableList.of(CalendarScopes.CALENDAR))
          .put("mail", ImmutableList.of(GmailScopes.GMAIL_MODIFY))
          .put("photos", ImmutableList.of("https://picasaweb.google.com/data/"))
          .put("tasks", ImmutableList.of(TasksScopes.TASKS))
          .put("contacts", ImmutableList.of(PeopleServiceScopes.CONTACTS))
          .build();

  // The scopes necessary to export each supported data type.
  // These should contain READONLY permissions
  private static final Map<String, List<String>> EXPORT_SCOPES =
      ImmutableMap.<String, List<String>>builder()
          .put("calendar", ImmutableList.of(CalendarScopes.CALENDAR_READONLY))
          .put("mail", ImmutableList.of(GmailScopes.GMAIL_READONLY))
          .put("photos", ImmutableList.of("https://www.googleapis.com/auth/photoslibrary.readonly"))
          .put("tasks", ImmutableList.of(TasksScopes.TASKS_READONLY))
          .put("contacts", ImmutableList.of(PeopleServiceScopes.CONTACTS_READONLY))
          .build();

  private final List<String> scopes;
  private final String redirectPath;
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;
  private final ObjectMapper objectMapper;

  /**
   * @param redirectPath the path part this generator is configured to request OAuth authentication
   *     code responses be sent to
   * @param appCredentials The Application credentials that the registration portal
   *     (console.cloud.google.com) assigned the portability instance
   * @param httpTransport The http transport to use for underlying GoogleAuthorizationCodeFlow
   * @param objectMapper The json factory provider
   */
  public GoogleAuthDataGenerator(
      String redirectPath,
      AppCredentials appCredentials,
      HttpTransport httpTransport,
      ObjectMapper objectMapper,
      String dataType,
      AuthMode mode) {

    this.redirectPath = redirectPath;
    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.objectMapper = objectMapper;
    this.scopes = mode == AuthMode.IMPORT ? IMPORT_SCOPES.get(dataType) : EXPORT_SCOPES.get(dataType);
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
            scopes)
        .setAccessType("offline")
        // TODO: Needed for local caching
        // .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
        .setApprovalPrompt("force")
        .build();
  }
}
