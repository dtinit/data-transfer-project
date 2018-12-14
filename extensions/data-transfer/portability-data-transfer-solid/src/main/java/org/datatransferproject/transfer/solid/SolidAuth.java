package org.datatransferproject.transfer.solid;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

class SolidAuth {
  private static String API_KEY = "asdf";
  private static String API_SECRET = "asdfsdf";

  private static String TOKEN_SERVER_URL = "https://inrupt.net/login";
  private static String AUTHORIZATION_SERVER_URL = "https://inrupt.net/authorize";

  private static final HttpTransport TRANSPORT = new NetHttpTransport();

  void tryLogin() throws Exception {
    AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken
        .authorizationHeaderAccessMethod(),
        TRANSPORT,
        new JacksonFactory(),
        new GenericUrl(TOKEN_SERVER_URL),
        new ClientParametersAuthentication(API_KEY, API_SECRET),
        API_KEY,
        AUTHORIZATION_SERVER_URL).build();
    // authorize
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost(
        "http://localhost").setPort(8080).build();
    System.out.println(flow
        .newAuthorizationUrl()
        .setRedirectUri("http://localhost:8080")
        .toString());
  }
}
