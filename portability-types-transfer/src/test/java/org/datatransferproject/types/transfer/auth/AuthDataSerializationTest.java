package org.datatransferproject.types.transfer.auth;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;

public class AuthDataSerializationTest {

  private ObjectMapper objectMapper;

  @Before
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
        "The read AuthData should be an instance of CookiesAndUrlAuthData",
        readValue instanceof CookiesAndUrlAuthData);
    final CookiesAndUrlAuthData readAuthData = (CookiesAndUrlAuthData) readValue;
    assertEquals("Expect cookies to be the same", cookies, readAuthData.getCookies());
    assertEquals("Expect url to be the same", url, readAuthData.getUrl());
  }

  @Test
  public void verifyTokenAuthData() throws IOException {
    final String token = "my_secret_token";
    final TokenAuthData authData = new TokenAuthData(token);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        "The read AuthData should be an instance of TokenAuthData",
        readValue instanceof TokenAuthData);
    final TokenAuthData readAuthData = (TokenAuthData) readValue;
    assertEquals("Expect token to be the same", token, readAuthData.getToken());
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
        "The read AuthData should be an instance of TokenAuthData",
        readValue instanceof TokensAndUrlAuthData);
    final TokensAndUrlAuthData readAuthData = (TokensAndUrlAuthData) readValue;
    assertEquals("Expect access token to be the same", accessToken, readAuthData.getAccessToken());
    assertEquals(
        "Expect refresh token to be the same", refreshToken, readAuthData.getRefreshToken());
    assertEquals("Expect url to be the same", url, readAuthData.getTokenServerEncodedUrl());
  }

  @Test
  public void verifyTokenSecretAuthData() throws IOException {
    final String token = "my_secret_token";
    final String secret = "my_secret";
    final TokenSecretAuthData authData = new TokenSecretAuthData(token, secret);

    final String s = objectMapper.writeValueAsString(authData);
    final AuthData readValue = objectMapper.readValue(s, AuthData.class);
    assertTrue(
        "The read AuthData should be an instance of TokenAuthData",
        readValue instanceof TokenSecretAuthData);
    final TokenSecretAuthData readAuthData = (TokenSecretAuthData) readValue;
    assertEquals("Expect token to be the same", token, readAuthData.getToken());
    assertEquals("Expect secret to be the same", secret, readAuthData.getSecret());
  }
}
