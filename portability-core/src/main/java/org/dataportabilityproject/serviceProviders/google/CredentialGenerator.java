package org.dataportabilityproject.serviceProviders.google;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;

/**
 * A generator of Google {@link Credential}
 */
class CredentialGenerator implements OfflineAuthDataGenerator {
  /** Port in the "Callback URL". */
  private static final int PORT = 8080;

  /** Domain name in the "Callback URL". */
  private static final String DOMAIN = "127.0.0.1";

  private final String clientId;
  private final String apiSecret;
  private final List<String> scopes;

  CredentialGenerator(String clientId, String apiSecret, List<String> scopes) {
      this.clientId = checkNotNull(clientId);
      this.apiSecret = checkNotNull(apiSecret);
    this.scopes = scopes;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    Credential c = authorize();

    return GoogleTokenData.create(
        c.getAccessToken(),
        c.getRefreshToken(),
        c.getTokenServerEncodedUrl());
  }

  Credential getCredential(AuthData authData) {
    checkArgument(authData instanceof GoogleTokenData,
        "authData expected to be TokenSecretAuthData not %s",
        authData.getClass().getCanonicalName());
    GoogleTokenData tokenData = (GoogleTokenData) authData;

    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
        .setTransport(GoogleStaticObjects.getHttpTransport())
        .setJsonFactory(JSON_FACTORY)
        .setClientAuthentication(new ClientParametersAuthentication(clientId, apiSecret))
        .setTokenServerEncodedUrl(tokenData.tokenServerEncodedUrl())
        .build()
        .setAccessToken(tokenData.accessToken())
        .setRefreshToken(tokenData.refreshToken())
        .setExpiresInSeconds(0L);
  }

  private Credential authorize() throws IOException {
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        GoogleStaticObjects.getHttpTransport(), JSON_FACTORY, clientId, apiSecret, scopes)
        .setAccessType("offline")
        .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
        .setApprovalPrompt("force")
        .build();
    // authorize
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
        .setHost(DOMAIN)
        .setPort(PORT)
        .build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }
}
