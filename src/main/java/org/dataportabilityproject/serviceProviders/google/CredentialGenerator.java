package org.dataportabilityproject.serviceProviders.google;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects.JSON_FACTORY;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import java.util.List;

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
      GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
          GoogleStaticObjects.getHttpTransport(), JSON_FACTORY, clientId, apiSecret, scopes)
              .setAccessType("offline")
              .setDataStoreFactory(GoogleStaticObjects.getDataStoreFactory())
              .setApprovalPrompt("force")
              .build();
        // authorize
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost(DOMAIN).setPort(PORT).build();
        return  new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
