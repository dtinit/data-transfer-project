package org.datatransferproject.types.transfer.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthDataSerializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(
        CookiesAndUrlAuthData.class,
        TokenAuthData.class,
        TokensAndUrlAuthData.class,
        TokenSecretAuthData.class);
  }

  @Test
  public void verifyCookiesAndUrlAuthData() throws IOException {
    final ArrayList<String> cookies = Lists.newArrayList("cookie_1", "cookie_2");
    final String url = "https://www.example.com/auth";
    final CookiesAndUrlAuthData authData = new CookiesAndUrlAuthData(cookies, url);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        readValue instanceof CookiesAndUrlAuthData,
        "The read AuthData should be an instance of CookiesAndUrlAuthData");
    final CookiesAndUrlAuthData readAuthData = (CookiesAndUrlAuthData) readValue;
    assertEquals(cookies, readAuthData.getCookies(), "Expect cookies to be the same");
    assertEquals(url, readAuthData.getUrl(), "Expect url to be the same");
  }

  @Test
  public void verifyTokenAuthData() throws IOException {
    final String token = "my_secret_token";
    final TokenAuthData authData = new TokenAuthData(token);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        readValue instanceof TokenAuthData,
        "The read AuthData should be an instance of TokenAuthData");
    final TokenAuthData readAuthData = (TokenAuthData) readValue;
    assertEquals(token, readAuthData.getToken(), "Expect token to be the same");
  }

  @Test
  public void verifyTokensAndUrlAuthData() throws IOException {
    final String accessToken = "my_access_token";
    final String refreshToken = "my_refresh_token";
    final String url = "https://www.example.com/auth";
    final TokensAndUrlAuthData authData = new TokensAndUrlAuthData(accessToken, refreshToken, url);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        readValue instanceof TokensAndUrlAuthData,
        "The read AuthData should be an instance of TokenAuthData");
    final TokensAndUrlAuthData readAuthData = (TokensAndUrlAuthData) readValue;
    assertEquals(accessToken, readAuthData.getAccessToken(), "Expect access token to be the same");
    assertEquals(
        refreshToken, readAuthData.getRefreshToken(), "Expect refresh token to be the same");
    assertEquals(url, readAuthData.getTokenServerEncodedUrl(), "Expect url to be the same");
  }

  @Test
  public void verifyTokenSecretAuthData() throws IOException {
    final String token = "my_secret_token";
    final String secret = "my_secret";
    final TokenSecretAuthData authData = new TokenSecretAuthData(token, secret);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        readValue instanceof TokenSecretAuthData,
        "The read AuthData should be an instance of TokenAuthData");
    final TokenSecretAuthData readAuthData = (TokenSecretAuthData) readValue;
    assertEquals(token, readAuthData.getToken(), "Expect token to be the same");
    assertEquals(secret, readAuthData.getSecret(), "Expect secret to be the same");
  }
}
