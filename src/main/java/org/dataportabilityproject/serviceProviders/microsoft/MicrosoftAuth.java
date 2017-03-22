package org.dataportabilityproject.serviceProviders.microsoft;

import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthorizationCodeInstalledAppSecureOverride;

/**
 * A work in progress to try to get authentication to MS working.
 */
public class MicrosoftAuth {
    /** Port in the "Callback URL". */
    private static final int PORT = 12345;

    /** Domain name in the "Callback URL". */
    private static final String DOMAIN = "localwebapp.com"; // Edit /etc/hosts to map this to localhost

    private static final String AUTHORIZATION_SERVER_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_SERVER_URL = "https://login.live.com/oauth20_token.srf";
    private final Secrets secrets;

    public MicrosoftAuth(Secrets secrets) {
        this.secrets = secrets;
    }

    public String getToken(String account) throws Exception {
        // set up authorization code flow
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(), // Access Method
                org.dataportabilityproject.serviceProviders.microsoft.MicrosoftStaticObjects.getHttpTransport(), // HttpTransport
                JSON_FACTORY, // JsonFactory
                new GenericUrl(TOKEN_SERVER_URL), // GenericUrl
                new ClientParametersAuthentication(
                        secrets.get("MICROSOFT_APP_ID"),
                        secrets.get("MICROSOFT_PASSWORD")), // HttpExecuteInterceptor 
                secrets.get("MICROSOFT_APP_ID"), // clientId
                AUTHORIZATION_SERVER_URL) // encoded url
            .setScopes(ImmutableList.of("wl.offline_access", "wl.calendars", "office.onenote")) // scopes
            .setDataStoreFactory(org.dataportabilityproject.serviceProviders.microsoft.MicrosoftStaticObjects.getDataStoreFactory()).build();
        
        // authorize
        // NOTE: This requires an https endpoint wired to forward requests to http://domain:port/Callback
        VerificationCodeReceiver receiver = new LocalServerReceiver.Builder().setHost(DOMAIN).setPort(PORT).build();
        Credential result =  new AuthorizationCodeInstalledAppSecureOverride(flow, receiver).authorize(account);
        return result.getAccessToken();
   }


    // TODO(chuy): Make clearing the data store an option
    private static void clearDataStore() throws IOException {
      System.out.println("Clearing data store");
      StoredCredential.getDefaultDataStore(org.dataportabilityproject.serviceProviders.microsoft.MicrosoftStaticObjects.getDataStoreFactory()).clear();
    }
}
