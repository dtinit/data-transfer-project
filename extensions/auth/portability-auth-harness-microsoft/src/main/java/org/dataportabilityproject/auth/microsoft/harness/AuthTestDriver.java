package org.dataportabilityproject.auth.microsoft.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dataportabilityproject.auth.microsoft.MicrosoftAuthDataGenerator;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import static java.lang.System.getProperty;

/**
 *
 */
public class AuthTestDriver {
    private String clientId;
    private String secret;

    private String callbackHost;
    private String callbackBase;
    private String authRetrievalUrl;

    public AuthTestDriver() {
        this.callbackHost = Objects.requireNonNull(getProperty("callbackHost"), "Callback host");
        String callbackPort = Objects.requireNonNull(getProperty("callbackPort"), "Callback port");
        this.clientId = Objects.requireNonNull(getProperty("clientId"), "Client ID");
        this.secret = Objects.requireNonNull(getProperty("secret"), "Client secret");

        callbackBase = "https://" + callbackHost + ":" + callbackPort;
        authRetrievalUrl = callbackBase + "/code";
    }

    public static void main(String... args) throws Exception {
        new AuthTestDriver().getOAuthTokenCode();
    }

    /**
     * Performs an OAuth flow using the MicrosoftAuthDataGenerator, returning a token.
     *
     * @return the token
     */
    public TokenAuthData getOAuthTokenCode() throws Exception {

        OkHttpClient client = TestHelper.createTestBuilder(callbackHost).build();
        ObjectMapper mapper = new ObjectMapper();

        MicrosoftAuthDataGenerator dataGenerator = new MicrosoftAuthDataGenerator("/response", () -> clientId, () -> secret, client, mapper);

        AuthFlowConfiguration configuration = dataGenerator.generateConfiguration(callbackBase, "1");

        Desktop desktop = Desktop.getDesktop();

        desktop.browse(new URI(configuration.getUrl()));

        // Execute the request and retrieve the auth code.
        String authCode = retrieveAuthCode(client);

        // get the token
        TokenAuthData tokenData = dataGenerator.generateAuthData(callbackBase, authCode, "1", configuration.getInitialAuthData(), null);

        System.out.println("TOKEN: " + tokenData.getToken());
        return tokenData;
    }

    private String retrieveAuthCode(OkHttpClient client) throws IOException {
        Request.Builder builder = new Request.Builder().url(authRetrievalUrl);

        try (Response authResponse = client.newCall(builder.build()).execute()) {
            ResponseBody authBody = authResponse.body();
            if (authBody == null) {
                throw new AssertionError("AUTH ERROR: " + authResponse.code() + ":" + "<empty body>");
            }
            String authCode = new String(authBody.bytes());

            System.out.println("AUTH: " + authResponse.code() + ":" + authCode);

            return authCode;
        }
    }


}
