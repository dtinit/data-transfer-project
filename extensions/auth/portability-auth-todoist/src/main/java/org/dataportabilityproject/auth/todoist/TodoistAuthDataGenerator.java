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

package org.dataportabilityproject.auth.todoist;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import java.util.Collections;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;

public class TodoistAuthDataGenerator implements AuthDataGenerator {
  private static final String CALLBACK_PATH = "/callback/todoist";
  private static final String AUTHORIZATION_SERVER_URL = "https://todoist.com/oauth/authorize";
  private static final String TOKEN_SERVER_URL = "https://todoist.com/oauth/access_token";
  private final AppCredentials appCredentials;
  private final HttpTransport httpTransport;
  private final String scope;

  public TodoistAuthDataGenerator(AppCredentials appCredentials, HttpTransport httpTransport, AuthMode mode) {
    this.appCredentials = appCredentials;
    this.httpTransport = httpTransport;
    this.scope = mode == AuthMode.IMPORT ? "data:read_write" : "data:read";
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
    return new AuthFlowConfiguration(url);
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    return null;
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
        .setScopes(Collections.singletonList(scope))
        // TODO: needed for local caching
        // .setDataStoreFactory(InstagramStaticObjects.getDataStoreFactory())
        .build();
  }
}
