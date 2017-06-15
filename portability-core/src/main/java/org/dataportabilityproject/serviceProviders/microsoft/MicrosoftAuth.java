package org.dataportabilityproject.serviceProviders.microsoft;

import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthorizationCodeInstalledAppSecureOverride;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;

/**
 * A work in progress to try to get authentication to MS working.
 */
final class MicrosoftAuth implements OfflineAuthDataGenerator {
    /** Port in the "Callback URL". */
    private static final int PORT = 12345;

    /** Domain name in the "Callback URL". */
    private static final String DOMAIN = "localwebapp.com"; // Edit /etc/hosts to map this to localhost

    private static final String AUTHORIZATION_SERVER_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_SERVER_URL = "https://login.live.com/oauth20_token.srf";
    private static final ImmutableList<String> SCOPES = ImmutableList.of(
        "wl.imap", // outlook export via IMAP
        "wl.offline_access", // provides for refresh tokens
        "wl.calendars", "wl.contacts_calendars"); // calendar export
    
    private final Secrets secrets;

    MicrosoftAuth(Secrets secrets) {
        this.secrets = secrets;
    }

    @Override
    public AuthData generateAuthData(IOInterface consoleIO) throws IOException {
        String account = consoleIO.ask("Enter Microsoft email account");
        String token = getToken(account);
        return MicrosoftOauthData.create(token, account);
    }

    /** Initiates the auth flow and obtains the access token. */
    private String getToken(String account) throws IOException {
        // set up authorization code flow
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(), // Access Method
                MicrosoftStaticObjects.getHttpTransport(), // HttpTransport
                JSON_FACTORY, // JsonFactory
                new GenericUrl(TOKEN_SERVER_URL), // GenericUrl
                new ClientParametersAuthentication(
                        secrets.get("MICROSOFT_APP_ID"),
                        secrets.get("MICROSOFT_PASSWORD")), // HttpExecuteInterceptor 
                secrets.get("MICROSOFT_APP_ID"), // clientId
                AUTHORIZATION_SERVER_URL) // encoded url
            .setScopes(SCOPES) // scopes
            .setDataStoreFactory(MicrosoftStaticObjects.getDataStoreFactory()).build();

        // authorize
        // NOTE: This requires an https endpoint wired to
        // forward requests to http://domain:port/Callback
        VerificationCodeReceiver receiver = new LocalServerReceiver.Builder()
            .setHost(DOMAIN).setPort(PORT).build();
        try {
          Credential result = new AuthorizationCodeInstalledAppSecureOverride(flow, receiver)
              .authorize(account);
          return result.getAccessToken();
        } catch (Exception e) {
          throw new IOException("Couldn't authorize", e);
        }
   }
}
