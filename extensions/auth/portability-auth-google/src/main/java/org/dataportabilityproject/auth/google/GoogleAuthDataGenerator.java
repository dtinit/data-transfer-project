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

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import java.io.IOException;
import java.util.function.Supplier;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/**
 * Provides configuration for conducting an OAuth flow against the Google AD API. Returned tokens
 * can be used to make requests against the Google Graph API.
 *
 * <p>The flow is a two-step process. First, the user is sent to an authorization page and is then
 * redirected to a address in this system with the authorization code. The second step takes the
 * authorization code and posts it against the AD API to obtain a token for querying the Graph API.
 */
public class GoogleAuthDataGenerator implements AuthDataGenerator {
  private final String redirectPath;
  private final Supplier<String> clientIdSupplier;
  private final Supplier<String> clientSecretSupplier;

  /**
   * @param redirectPath the path part this generator is configured to request OAuth authentication
   *     code responses be sent to
   * @param clientIdSupplier The Application ID that the registration portal
   *     (apps.dev.microsoft.com) assigned the portability instance
   * @param clientSecretSupplier The application secret that was created in the app registration
   *     portal for the portability instance
   */
  public GoogleAuthDataGenerator(
      String redirectPath,
      Supplier<String> clientIdSupplier,
      Supplier<String> clientSecretSupplier

  ) {
    this.redirectPath = redirectPath;
    this.clientIdSupplier = clientIdSupplier;
    this.clientSecretSupplier = clientSecretSupplier;
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    String encodedJobId = JobUtils.encodeJobId(jobId);
    String url =
        createFlow()
            .newAuthorizationUrl()
            .setRedirectUri(callbackBaseUrl + CALLBACK_PATH)
            .setState(encodedJobId) // TODO: Encrypt
            .build();
    // constructs a request for Google authorization code.
    String redirectUrl = callbackBaseUrl + redirectPath;
    String queryPart =
        constructAuthQueryPart(
            redirectUrl, id, "user.read", "mail.read", "Contacts.ReadWrite", "Calendars.ReadWrite");
    return new AuthFlowConfiguration(AUTHORIZATION_URL + "?" + queryPart);
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    return null;
  }

  /** Creates an AuthorizationCodeFlow for use in online and offline mode. */
  private GoogleAuthorizationCodeFlow createFlow() throws IOException {
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
