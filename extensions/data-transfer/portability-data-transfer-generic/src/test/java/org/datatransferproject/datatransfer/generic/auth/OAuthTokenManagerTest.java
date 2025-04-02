package org.datatransferproject.datatransfer.generic.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OAuthTokenManagerTest {
  private MockWebServer webServer;
  private AppCredentials appCredentials = new AppCredentials("appKey", "appSecret");
  private Monitor monitor = new Monitor() {};

  @Before
  public void setup() throws IOException {
    webServer = new MockWebServer();
    webServer.start();
  }

  @After
  public void teardown() throws IOException {
    webServer.shutdown();
  }

  private TokensAndUrlAuthData getInitialAuthData() {
    return new TokensAndUrlAuthData(
        "initialAccessToken", "refreshToken", webServer.url("/refresh").toString());
  }

  @Test
  public void testWithAuthDataNoRefresh() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);

    TokensAndUrlAuthData usedAuthData = tokenManager.withAuthData(authData -> authData);
    assertEquals(initialAuthData.getToken(), usedAuthData.getToken());
  }

  @Test
  public void testWithAuthDataWithRefresh() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                ""
                    + "{"
                    + "  \"access_token\": \"newAccessToken\","
                    + "  \"token_type\": \"Bearer\""
                    + "}"));

    TokensAndUrlAuthData usedAuthData =
        tokenManager.withAuthData(
            authData -> {
              if (authData.equals(initialAuthData)) {
                throw new InvalidTokenException("Token expired", null);
              }
              return authData;
            });

    assertEquals("newAccessToken", usedAuthData.getToken());
    assertEquals("refreshToken", usedAuthData.getRefreshToken());
    assertEquals(1, webServer.getRequestCount());
    RecordedRequest request = webServer.takeRequest();
    assertEquals(
        "grant_type=refresh_token&client_id=appKey&client_secret=appSecret&refresh_token=refreshToken",
        new String(request.getBody().readByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testWithAuthDataWithRefreshedRefreshToken() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                ""
                    + "{"
                    + "  \"access_token\": \"newAccessToken\","
                    + "  \"refresh_token\": \"newRefreshToken\","
                    + "  \"token_type\": \"Bearer\","
                    + "  \"some_random_field\": \"some_random_value\""
                    + "}"));

    TokensAndUrlAuthData usedAuthData =
        tokenManager.withAuthData(
            authData -> {
              if (authData.equals(initialAuthData)) {
                throw new InvalidTokenException("Token expired", null);
              }
              return authData;
            });

    assertEquals("newRefreshToken", usedAuthData.getRefreshToken());
  }

  @Test
  public void testWithAuthDataWithRefreshFailure() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(
        new MockResponse().setResponseCode(400).setBody("{\"error\": \"invalid_token\"}"));

    assertThrows(
        "invalid_token",
        IOException.class,
        () ->
            tokenManager.withAuthData(
                authData -> {
                  if (authData.equals(initialAuthData)) {
                    throw new InvalidTokenException("Token expired", null);
                  }
                  return authData;
                }));
  }

  @Test
  public void testWithAuthDataWithRefreshSuccessWithUnexpectedResponse() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(new MockResponse().setResponseCode(200).setBody("invalidresponsebody"));

    assertThrows(
        "invalidresponsebody",
        IOException.class,
        () ->
            tokenManager.withAuthData(
                authData -> {
                  if (authData.equals(initialAuthData)) {
                    throw new InvalidTokenException("Token expired", null);
                  }
                  return authData;
                }));
  }

  @Test
  public void testWithAuthDataWithRefreshNewTokenStillInvalid() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                ""
                    + "{"
                    + "  \"access_token\": \"newAccessToken\","
                    + "  \"token_type\": \"Bearer\""
                    + "}"));

    assertThrows(
        "Token still expired after refresh",
        InvalidTokenException.class,
        () ->
            tokenManager.withAuthData(
                authData -> {
                  throw new InvalidTokenException("Token expired", null);
                }));

    // Only try refreshing once
    assertEquals(1, webServer.getRequestCount());
  }

  @Test
  public void testWithAuthDataWithRefreshPreservesNewToken() throws Exception {
    TokensAndUrlAuthData initialAuthData = getInitialAuthData();
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);
    webServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                ""
                    + "{"
                    + "  \"access_token\": \"newAccessToken\","
                    + "  \"token_type\": \"Bearer\""
                    + "}"));

    TokensAndUrlAuthData usedAuthData =
        tokenManager.withAuthData(
            authData -> {
              if (authData.equals(initialAuthData)) {
                throw new InvalidTokenException("Token expired", null);
              }
              return authData;
            });
    assertEquals("newAccessToken", usedAuthData.getToken());
    assertEquals(1, webServer.getRequestCount());

    TokensAndUrlAuthData secondUsedAuthData = tokenManager.withAuthData(authData -> authData);
    assertEquals("newAccessToken", secondUsedAuthData.getToken());
    // Still only 1 request to refresh
    assertEquals(1, webServer.getRequestCount());
  }

  @Test
  public void testWithAuthDataNoRefreshToken() throws Exception {
    TokensAndUrlAuthData initialAuthData = new TokensAndUrlAuthData("initialAccessToken", null, "");
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(initialAuthData, appCredentials, new OkHttpClient(), monitor);

    assertThrows(
        "Token expired",
        InvalidTokenException.class,
        () ->
            tokenManager.withAuthData(
                authData -> {
                  if (authData.equals(initialAuthData)) {
                    throw new InvalidTokenException("Token expired", null);
                  }
                  return authData;
                }));
    assertEquals(0, webServer.getRequestCount());
  }

  @Test
  public void testWithAuthDataPropagatesIOExceptions() throws Exception {
    OAuthTokenManager tokenManager =
        new OAuthTokenManager(getInitialAuthData(), appCredentials, new OkHttpClient(), monitor);

    assertThrows(
        "messagetopropagate",
        IOException.class,
        () ->
            tokenManager.withAuthData(
                authData -> {
                  throw new IOException("messagetopropagate");
                }));
  }
}
