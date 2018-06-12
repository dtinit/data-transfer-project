package org.dataportabilityproject.auth.microsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provides configuration for conducting an OAuth flow against the Microsoft AD API. Returned tokens
 * can be used to make requests against the Microsoft Graph API.
 *
 * <p>The flow is a two-step process. First, the user is sent to an authorization page and is then
 * redirected to a address in this system with the authorization code. The second step takes the
 * authorization code and posts it against the AD API to obtain a token for querying the Graph API.
 */
public class MicrosoftAuthDataGenerator implements AuthDataGenerator {
  private static final String AUTHORIZATION_URL =
      "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
  private static final String TOKEN_URL =
      "https://login.microsoftonline.com/common/oauth2/v2.0/token";

  // The scopes necessary to import each supported data type.
  // These are READ/WRITE scopes
  private static final Map<String, List<String>> importAuthScopes =
      ImmutableMap.<String, List<String>>builder()
          .put("mail", ImmutableList.of("user.read", "Mail.ReadWrite"))
          .put("contacts", ImmutableList.of("user.read", "Contacts.ReadWrite"))
          .put("calendar", ImmutableList.of("user.read", "Calendars.ReadWrite"))
          .build();

  // The scopes necessary to export each supported data type.
  // These should contain READONLY permissions
  private static final Map<String, List<String>> exportAuthScopes =
      ImmutableMap.<String, List<String>>builder()
          .put("mail", ImmutableList.of("user.read", "Mail.Read"))
          .put("contacts", ImmutableList.of("user.read", "Contacts.Read"))
          .put("calendar", ImmutableList.of("user.read", "Calendars.Read"))
          .put("derived-data", ImmutableList.of("user.read", "Files.Read.All"))
          .build();

  private final String redirectPath;
  private final Supplier<String> clientIdSupplier;
  private final Supplier<String> clientSecretSupplier;
  private final OkHttpClient httpClient;
  private final ObjectMapper mapper;
  private final List<String> scopes;

  /**
   * Ctor.
   *
   * @param redirectPath the path part this generator is configured to request OAuth authentication
   *     code responses be sent to
   * @param clientIdSupplier The Application ID that the registration portal
   *     (apps.dev.microsoft.com) assigned the portability instance
   * @param clientSecretSupplier The application secret that was created in the app registration
   *     portal for the portability instance
   * @param mapper the mapper for deserializing responses from JSON
   * @param transferDataType the data type to create this authorization generator for
   * @param mode the mode to create this authorization generator for
   */
  public MicrosoftAuthDataGenerator(
      String redirectPath,
      Supplier<String> clientIdSupplier,
      Supplier<String> clientSecretSupplier,
      OkHttpClient client,
      ObjectMapper mapper,
      String transferDataType,
      AuthMode mode) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(transferDataType) && mode != null,
        "A valid mode and transfer data type must be present");
    this.redirectPath = redirectPath;
    this.clientIdSupplier = clientIdSupplier;
    this.clientSecretSupplier = clientSecretSupplier;
    httpClient = client;
    this.mapper = mapper;
    this.scopes =
        mode == AuthMode.EXPORT
            ? exportAuthScopes.get(transferDataType)
            : importAuthScopes.get(transferDataType);
  }

  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // constructs a request for the Microsoft Graph authorization code.
    String redirectUrl = callbackBaseUrl + redirectPath;
    String queryPart = constructAuthQueryPart(redirectUrl, id, scopes);
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

  private String constructAuthQueryPart(String redirectUrl, String id, List<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
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
