package org.dataportabilityproject.serviceProviders.microsoft;

import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.GenericUrl;
import com.google.common.collect.ImmutableList;
import java.util.Scanner;
import org.dataportabilityproject.shared.Secrets;

/**
 * A work in progress to try to get authentication to MS working.
 */
public class MicrosoftAuth {
    /** Port in the "Callback URL". */
    private static final int PORT = 12345;

    /** Domain name in the "Callback URL". */
    private static final String DOMAIN = "localwebapp.com"; // "127.0.0.1";

    //public static final String AUTHORIZATION_SERVER_URL = " https://login.windows.net/common/oauth2/authorize";
    //public static final String TOKEN_SERVER_URL = "https://login.windows.net/common/oauth2/token";

    private static final String AUTHORIZATION_SERVER_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_SERVER_URL = "https://login.live.com/oauth20_token.srf";
    private final Secrets secrets;

    public MicrosoftAuth(Secrets secrets) {
        this.secrets = secrets;
    }

    public String getToken() throws Exception {
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
            .setScopes(ImmutableList.of("wl.offline_access")) // scopes
            .setDataStoreFactory(org.dataportabilityproject.serviceProviders.microsoft.MicrosoftStaticObjects.getDataStoreFactory()).build();
        
        // authorize
        // Web app requires an endpoint 
        // TODO(holachuy): Determine if Jetty can be configured to send https redirect_uri and handle it
        VerificationCodeReceiver receiver = new LocalServerReceiver.Builder().setHost(DOMAIN).setPort(PORT).build();

        Credential result =  new AuthorizationCodeInstalledApp(flow, receiver).authorize("jchavezbaz@live.com");
        System.out.println(result.getAccessToken());
        return result.getAccessToken();
   }
}
