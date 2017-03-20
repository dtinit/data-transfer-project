package org.dataportabilityproject.serviceProviders.google;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;

import java.util.List;

import static com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.AUTHORIZATION_SERVER_URL;
import static com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.TOKEN_SERVER_URL;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

/**
 * A generator of Google {@link Credential}
 */
public class CredentialGenerator {
    /** Port in the "Callback URL". */
    public static final int PORT = 8080;

    /** Domain name in the "Callback URL". */
    public static final String DOMAIN = "127.0.0.1";

    private final String clientId;
    private final String apiSecret;

    public CredentialGenerator(String clientId, String apiSecret) {
        this.clientId = checkNotNull(clientId);
        this.apiSecret = checkNotNull(apiSecret);
    }

    public Credential authorize(List<String> scopes) throws Exception {
        // set up authorization code flow
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken
                .authorizationHeaderAccessMethod(),
                GoogleStaticObjects.getHttpTransport(),
                JSON_FACTORY,
                new GenericUrl(TOKEN_SERVER_URL),
                new ClientParametersAuthentication(clientId, apiSecret),
                clientId,
                AUTHORIZATION_SERVER_URL).setScopes(scopes)
                .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory()).build();
        // authorize
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost(DOMAIN).setPort(PORT).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
