package org.dataportabilityproject.serviceProviders.instagram;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.SecretAuthData;


final class InastagramAuth implements OfflineAuthDataGenerator {
  private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

  private final String clientId;
  private final String clientSecret;

  InastagramAuth(String clientId, String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
      AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
          BearerToken.authorizationHeaderAccessMethod(), // Access Method
          getTransport(),
          JSON_FACTORY,
          new GenericUrl("https://api.instagram.com/oauth/access_token"), // GenericUrl
          new ClientParametersAuthentication(
              clientId,
              clientSecret),
          clientId, // clientId
          "https://api.instagram.com/oauth/authorize/") // encoded url
          .setScopes(ImmutableList.of("basic", "public_content")) // scopes
          .build();

      VerificationCodeReceiver receiver = new LocalServerReceiver.Builder()
          .setHost("localhost").setPort(12345).build();
      try {
        Credential result = new AuthorizationCodeInstalledApp(flow, receiver)
            .authorize("user");
        return SecretAuthData.create(result.getAccessToken());
      } catch (Exception e) {
        throw new IOException("Couldn't authorize", e);
      }
    }

  private synchronized NetHttpTransport getTransport() {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Problem getting token", e);
    }
  }
}
