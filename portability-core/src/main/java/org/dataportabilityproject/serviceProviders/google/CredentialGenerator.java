package org.dataportabilityproject.serviceProviders.google;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthRequest;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * A generator of Google {@link Credential}
 */
class CredentialGenerator implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {
  /** Port in the "Callback URL". */
  private static final int PORT = 8080;
  private static final String CALLBACK_URL = "http://localhost:8080/callback/google";

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
    return toAuthData(c);
  }

  @Override
  public AuthRequest generateAuthUrl(String id) throws IOException {
    String url = createFlow(clientId, apiSecret, scopes)
        .newAuthorizationUrl()
        .setRedirectUri(CALLBACK_URL)
        .setState(id) // TODO: Encrypt
        .build();
    return AuthRequest.create(url);
  }

  @Override
  public AuthData generateAuthData(String authCode, String id, @Nullable AuthData initialAuthData) throws IOException {
    Preconditions.checkState(initialAuthData == null, "Earlier auth data not expected for Google flow");
    AuthorizationCodeFlow flow = createFlow(clientId, apiSecret, scopes);
    String redirectAfterToken = "http://localhost:8080/callback/google";
    System.out.println("redirectAfterToken: " + redirectAfterToken);
    TokenResponse response = flow
        .newTokenRequest(authCode)
        .setRedirectUri(redirectAfterToken) //TODO(chuy): Parameterize
        .execute();
    // Figure out storage
    Credential credential = flow.createAndStoreCredential(response, id);
    // Extract the Google User ID from the ID token in the auth response
    // GoogleIdToken.Payload payload = ((GoogleTokenResponse) response).parseIdToken().getPayload();
    return toAuthData(credential);
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

  private AuthData toAuthData(Credential credential) {
    return GoogleTokenData.create(
        credential.getAccessToken(),
        credential.getRefreshToken(),
        credential.getTokenServerEncodedUrl());
  }

  private Credential authorize() throws IOException {
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow = createFlow(clientId, apiSecret, scopes);

    // authorize
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
        .setHost(DOMAIN)
        .setPort(PORT)
        .build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  /** Creates an AuthorizationCodeFlow for use in online and offline mode.*/
  private static GoogleAuthorizationCodeFlow createFlow(String clientId, String secret,
      List<String> scopes)
      throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(
        GoogleStaticObjects.getHttpTransport(), JSON_FACTORY, clientId, secret, scopes)
        .setAccessType("offline")
        .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
        .setApprovalPrompt("force")
        .build();
  }
}
