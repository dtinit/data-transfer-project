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
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;

/**
 * Provides configuration for conducting an OAuth flow against the Google AD API. Returned tokens
 * can be used to make requests against the Google Graph API.
 *
 * <p>The flow is a two-step process. First, the user is sent to an authorization page and is then
 * redirected to a address in this system with the authorization code. The second step takes the
 * authorization code and posts it against the AD API to obtain a token for querying the Graph API.
 */
public class GoogleAuthDataGenerator implements AuthDataGenerator {
  private static final String AUTHORIZATION_URL =
      "https://login.Googleonline.com/common/oauth2/v2.0/authorize";
  private static final String TOKEN_URL =
      "https://login.Googleonline.com/common/oauth2/v2.0/token";

  private final String redirectPath;
  private final Supplier<String> clientIdSupplier;
  private final Supplier<String> clientSecretSupplier;
  private final OkHttpClient httpClient;
  private final ObjectMapper mapper;

  /**
   * Ctor.
   *
   * @param redirectPath the path part this generator is configured to request OAuth authentication
   *     code responses be sent to
   * @param clientIdSupplier The Application ID that the registration portal
   *     (apps.dev.Google.com) assigned the portability instance
   * @param clientSecretSupplier The application secret that was created in the app registration
   *     portal for the portability instance
   * @param mapper the mapper for deserializing responses from JSON
   */
  public GoogleAuthDataGenerator(
      String redirectPath,
      Supplier<String> clientIdSupplier,
      Supplier<String> clientSecretSupplier,
      OkHttpClient client,
      ObjectMapper mapper) {
    this.redirectPath = redirectPath;
    this.clientIdSupplier = clientIdSupplier;
    this.clientSecretSupplier = clientSecretSupplier;
    httpClient = client;
    this.mapper = mapper;
  }

  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // constructs a request for the Google Graph authorization code.
    String redirectUrl = callbackBaseUrl + redirectPath;
    String queryPart =
        constructAuthQueryPart(
            redirectUrl, id, "user.read", "mail.read", "Contacts.ReadWrite", "Calendars.ReadWrite");
x


    String encodedJobId = JobUtils.encodeJobId(jobId);
    String url =
        createFlow()
            .newAuthorizationUrl()
            .setRedirectUri(callbackBaseUrl + CALLBACK_PATH)
            .setState(encodedJobId) // TODO: Encrypt
            .build();
    return AuthFlowInitiator.create(url);
  }


    return new AuthFlowConfiguration(AUTHORIZATION_URL + "?" + queryPart);
  }

  public TokenAuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    String redirectUrl = callbackBaseUrl + redirectPath;
    String params = constructTokenParams(authCode, redirectUrl, "user.read", "Contacts.ReadWrite");

    Request.Builder tokenReqBuilder = new Request.Builder().url(TOKEN_URL);
    tokenReqBuilder.header("Content-Type", "application/x-www-form-urlencoded");
    tokenReqBuilder.post(
        RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), params));
    try (Response tokenResponse = httpClient.newCall(tokenReqBuilder.build()).execute()) {
      ResponseBody tokenBody = tokenResponse.body();
      if (tokenBody == null) {
        // FIXME we need a way to report back errors creating a token, perhaps by changing the
        // return type
        throw new RuntimeException(
            "Token body was null:" + tokenResponse.code() + ":" + "<empty body>");
      }
      String token = new String(tokenBody.bytes());
      Map<String, String> map = mapper.reader().forType(Map.class).readValue(token);
      String accessToken = map.get("access_token");
      if (accessToken == null) {
        // FIXME we need a way to report back errors creating a token, perhaps by changing the
        // return type
        throw new RuntimeException(
            "Token body was null:" + tokenResponse.code() + ":" + "Not in params");
      }
      return new TokenAuthData(accessToken);
    } catch (IOException e) {
      // FIXME we need a way to report back errors creating a token, perhaps by changing the return
      // type
      throw new RuntimeException(e);
    }
  }

  private String constructAuthQueryPart(String redirectUrl, String id, String... scopes) {
    if (scopes == null || scopes.length == 0) {
      throw new IllegalArgumentException("At least one OAuth scope must be specified");
    }
    ParamStringBuilder builder = new ParamStringBuilder();

    String clientId = clientIdSupplier.get();
    builder.startParam("client_id").value(clientId).endParam();

    builder.startParam("scope");
    for (String scope : scopes) {
      builder.value(scope);
    }
    builder.endParam();

    builder.startParam("redirect_uri").value(redirectUrl).endParam();
    builder.startParam("grant_type").value("authorization_code").endParam();
    builder.startParam("response_type").value("code").endParam();
    builder.startParam("state").value(id).endParam();
    return builder.build();
  }

  private String constructTokenParams(String authCode, String redirectUrl, String... scopes) {
    if (scopes == null || scopes.length == 0) {
      throw new IllegalArgumentException("At least one OAuth scope must be specified");
    }
    ParamStringBuilder builder = new ParamStringBuilder();

    String clientId = clientIdSupplier.get();
    builder.startParam("client_id").value(clientId).endParam();

    builder.startParam("code").value(authCode).endParam();
    builder.startParam("client_secret").value(clientSecretSupplier.get()).endParam();

    builder.startParam("scope");
    for (String scope : scopes) {
      builder.value(scope);
    }
    builder.endParam();

    builder.startParam("redirect_uri").value(redirectUrl).endParam();
    builder.startParam("grant_type").value("authorization_code").endParam();
    return builder.build();
  }
}
