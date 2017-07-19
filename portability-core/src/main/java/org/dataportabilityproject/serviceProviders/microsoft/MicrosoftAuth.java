package org.dataportabilityproject.serviceProviders.microsoft;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthRequest;
import org.dataportabilityproject.shared.auth.AuthorizationCodeInstalledAppSecureOverride;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * Implements authorization flow for Microsoft
 */
final class MicrosoftAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {
    /** Port in the "Callback URL". */
    private static final int PORT = 12345;

    /** Domain name in the "Callback URL". */
    private static final String CALLBACK_URL = "https://localhost:8080/callback/microsoft";
    private static final String DOMAIN = "localwebapp.com"; // Edit /etc/hosts to map this to localhost
    private static final String AUTHORIZATION_SERVER_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_SERVER_URL = "https://login.live.com/oauth20_token.srf";

    private final String clientId;
    private final String apiSecret;
    private final List<String> scopes;
    
    MicrosoftAuth(String clientId, String apiSecret, List<String> scopes) {
        this.clientId = checkNotNull(clientId);
        this.apiSecret = checkNotNull(apiSecret);
        Preconditions.checkArgument(!scopes.isEmpty(), "At least one scope is required.");
        this.scopes = scopes;
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
    public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData)
        throws IOException {
        Preconditions.checkState(initialAuthData == null, "Earlier auth data not expected for Google flow");
        AuthorizationCodeFlow flow = createFlow(clientId, apiSecret, scopes);
        TokenResponse response = flow
            .newTokenRequest(authCode)
            .setRedirectUri(CALLBACK_URL) //TODO(chuy): Parameterize
            .execute();
        // Figure out storage
        Credential credential = flow.createAndStoreCredential(response, id);
        // Extract the Google User ID from the ID token in the auth response
        // GoogleIdToken.Payload payload = ((GoogleTokenResponse) response).parseIdToken().getPayload();
        return toAuthData(credential);
    }

    private static MicrosoftOauthData toAuthData(Credential credential) {
        return MicrosoftOauthData.create(
            credential.getAccessToken(),
            credential.getRefreshToken(),
            credential.getTokenServerEncodedUrl(),
            "TBD-account@live.com"); // TODO: Remove if not needed
    }

    @Override
    public AuthData generateAuthData(IOInterface consoleIO) throws IOException {
        String account = consoleIO.ask("Enter Microsoft email account");
        return getAuthData(account);
    }

    /** Initiates the auth flow and obtains the access token. */
    private MicrosoftOauthData getAuthData(String account) throws IOException {
        // set up authorization code flow
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(), // Access Method
                MicrosoftStaticObjects.getHttpTransport(), // HttpTransport
                JSON_FACTORY, // JsonFactory
                new GenericUrl(TOKEN_SERVER_URL), // GenericUrl
                new ClientParametersAuthentication(clientId, apiSecret), // HttpExecuteInterceptor
                clientId, // clientId
                AUTHORIZATION_SERVER_URL) // encoded url
            .setScopes(scopes) // scopes
            .setDataStoreFactory(MicrosoftStaticObjects.getDataStoreFactory()).build();

        // authorize
        // NOTE: This requires an https endpoint wired to
        // forward requests to http://domain:port/Callback
        VerificationCodeReceiver receiver = new LocalServerReceiver.Builder()
            .setHost(DOMAIN).setPort(PORT).build();
        try {
          Credential credential = new AuthorizationCodeInstalledAppSecureOverride(flow, receiver)
              .authorize(account);
          return toAuthData(credential);
        } catch (Exception e) {
          throw new IOException("Couldn't authorize", e);
        }
   }

    /** Creates an AuthorizationCodeFlow for use in online and offline mode.*/
    private static AuthorizationCodeFlow createFlow(String clientId, String apiSecret,
        List<String> scopes)
        throws IOException {
        // set up authorization code flow
        return new AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(), // Access Method
            MicrosoftStaticObjects.getHttpTransport(), // HttpTransport
            JSON_FACTORY, // JsonFactory
            new GenericUrl(TOKEN_SERVER_URL), // GenericUrl
            new ClientParametersAuthentication(clientId, apiSecret), // HttpExecuteInterceptor
            clientId, // clientId
            AUTHORIZATION_SERVER_URL) // encoded url
            .setScopes(scopes) // scopes
            .setDataStoreFactory(MicrosoftStaticObjects.getDataStoreFactory()).build();
    }
}
