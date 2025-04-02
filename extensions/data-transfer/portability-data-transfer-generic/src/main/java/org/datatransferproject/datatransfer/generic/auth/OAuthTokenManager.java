package org.datatransferproject.datatransfer.generic.auth;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.Nullable;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class RefreshTokenResponse {
  private final String accessToken;
  private final Optional<String> refreshToken;
  private final String tokenType;
  private final Optional<String> expiresIn;

  @JsonCreator
  public RefreshTokenResponse(
      @JsonProperty(value = "access_token", required = true) String accessToken,
      @Nullable @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty(value = "token_type", required = true) String tokenType,
      @Nullable @JsonProperty("expires_in") String expiresIn) {
    this.accessToken = accessToken;
    this.refreshToken = Optional.ofNullable(refreshToken);
    this.tokenType = tokenType;
    this.expiresIn = Optional.ofNullable(expiresIn);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public Optional<String> getRefreshToken() {
    return refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public Optional<String> getExpiresIn() {
    return expiresIn;
  }
}

/**
 * Utility to manage {@link TokensAndUrlAuthData} containing OAuth refresh and access tokens.<br>
 * See {@see #withAuthData} for how to use the auth data managed by this class.
 */
public class OAuthTokenManager {
  @FunctionalInterface
  public interface FunctionRequiringAuthData<T, Ex extends Exception> {
    public T execute(TokensAndUrlAuthData authData) throws Ex, InvalidTokenException;
  }

  AppCredentials appCredentials;
  TokensAndUrlAuthData authData;

  OkHttpClient client;
  Monitor monitor;
  ObjectMapper om = new ObjectMapper(new JsonFactory());

  static final MediaType JSON = MediaType.parse("application/json");
  static final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");

  /**
   * @param initialAuthData The auth data to be used for the first request
   * @param appCredentials containing the OAuth client ID and client secret
   * @param client to use for making HTTP requests when refreshing the token
   * @param monitor for logging
   */
  public OAuthTokenManager(
      TokensAndUrlAuthData initialAuthData,
      AppCredentials appCredentials,
      OkHttpClient client,
      Monitor monitor) {
    this.appCredentials = appCredentials;
    this.authData = initialAuthData;

    this.client = client;
    this.monitor = monitor;
    this.om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    this.om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private TokensAndUrlAuthData refreshToken() throws IOException {
    monitor.info(() -> "Refreshing OAuth token");
    Request request =
        new Request.Builder()
            .url(authData.getTokenServerEncodedUrl())
            .addHeader("Accept", JSON.toString())
            .post(
                new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", appCredentials.getKey())
                    .add("client_secret", appCredentials.getSecret())
                    .add("refresh_token", authData.getRefreshToken())
                    .build())
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.code() >= 400) {
        throw new IOException(
            format(
                "Error while refreshing token (%d): %s",
                response.code(), new String(response.body().bytes(), StandardCharsets.UTF_8)));
      }
      byte[] body = response.body().bytes();
      RefreshTokenResponse responsePayload;
      try {
        responsePayload = om.readValue(body, RefreshTokenResponse.class);
      } catch (JsonParseException | JsonMappingException e) {
        throw new IOException(
            format(
                "Unexpected response while refreshing token: %s",
                new String(body, StandardCharsets.UTF_8)),
            e);
      }
      return new TokensAndUrlAuthData(
          responsePayload.getAccessToken(),
          responsePayload.getRefreshToken().orElse(authData.getRefreshToken()),
          authData.getTokenServerEncodedUrl());
    }
  }

  /**
   * Call a function {@code f} requiring auth data, injecting the auth data. If the function raises
   * an {@link InvalidTokenException}, the token will be refreshed and the function will be called
   * again with the fresh token.
   *
   * @param f The function to call - {@code (authData) -> T}
   * @param <T> {@code f}'s return type
   * @return The {@code <T>} returned by {@code f}
   * @throws IOException if {@code f} throws an {@link IOException}
   * @throws InvalidTokenException if {@code f} throws an {@link InvalidTokenException} after the
   *     access token has been refreshed
   */
  public <T, Ex extends Exception> T withAuthData(FunctionRequiringAuthData<T, Ex> f)
      throws Ex, InvalidTokenException, IOException {
    try {
      return f.execute(authData);
    } catch (InvalidTokenException e) {
      if (authData.getRefreshToken() == null || authData.getRefreshToken().isEmpty()) {
        monitor.severe(() -> "Refresh token not present with auth data");
        throw e;
      }

      this.authData = refreshToken();
      try {
        return f.execute(this.authData);
      } catch (InvalidTokenException innerException) {
        throw new InvalidTokenException("Token still expired after refresh", innerException);
      }
    }
  }
}
