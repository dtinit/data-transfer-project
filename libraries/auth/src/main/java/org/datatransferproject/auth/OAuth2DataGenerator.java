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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol.OAUTH_2;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.client.utils.URIBuilder;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 * General implementation of an {@link AuthDataGenerator} for OAuth2.
 */
public class OAuth2DataGenerator implements AuthDataGenerator {

  private final OAuth2Config config;
  private final Set<String> scopes;
  // TODO: handle dynamic updates of client ids and secrets #597
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;

  OAuth2DataGenerator(OAuth2Config config, AppCredentials appCredentials,
      HttpTransport httpTransport,
      String dataType, AuthMode authMode) {
    this.config = config;
    validateConfig();
    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.scopes = authMode == AuthMode.EXPORT
        ? config.getExportScopes().get(dataType)
        : config.getImportScopes().get(dataType);
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    String encodedJobId = BaseEncoding.base64Url().encode(id.getBytes(UTF_8));
    String scope = scopes.isEmpty() ? "" : String.join(" ", scopes);
    try {
      URIBuilder builder = new URIBuilder(config.getAuthUrl())
          .setParameter("response_type", "code")
          .setParameter("client_id", clientId)
          .setParameter("redirect_uri", callbackBaseUrl)
          .setParameter("scope", scope)
          .setParameter("state", encodedJobId);

      if (config.getAdditionalAuthUrlParameters() != null) {
        for (Entry<String, String> entry : config.getAdditionalAuthUrlParameters().entrySet()) {
          builder.setParameter(entry.getKey(), entry.getValue());
        }
      }

      String url = builder.build().toString();
      return new AuthFlowConfiguration(url, OAUTH_2, getTokenUrl());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Could not produce url.", e);
    }
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(
        Strings.isNullOrEmpty(extra), "Extra data not expected for OAuth flow");
    Preconditions.checkArgument(initialAuthData == null,
        "Initial auth data not expected for " + config.getServiceName());

    Map<String, String> params = new LinkedHashMap<>();
    params.put("client_id", clientId);
    params.put("client_secret", clientSecret);
    params.put("grant_type", "authorization_code");
    params.put("redirect_uri", callbackBaseUrl);
    params.put("code", authCode);

    HttpContent content = new UrlEncodedContent(params);

    try {
      String tokenResponse = OAuthUtils.makeRawPostRequest(
          httpTransport, config.getTokenUrl(), content);

      return config.getResponseClass(tokenResponse);
    } catch (IOException e) {
      throw new RuntimeException("Error getting token", e); // TODO
    }
  }

  private void validateConfig() {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getServiceName()),
        "Config is missing service name");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(config.getAuthUrl()), "Config is missing auth url");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(config.getTokenUrl()), "Config is missing token url");

    // This decision is not OAuth spec, but part of an effort to prevent accidental scope omission
    Preconditions
        .checkArgument(config.getExportScopes() != null, "Config is missing export scopes");
    Preconditions
        .checkArgument(config.getImportScopes() != null, "Config is missing import scopes");
  }
}