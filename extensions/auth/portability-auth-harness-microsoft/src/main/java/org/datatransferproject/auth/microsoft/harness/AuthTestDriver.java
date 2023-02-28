package org.datatransferproject.auth.microsoft.harness;

import static java.lang.System.getProperty;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.auth.microsoft.MicrosoftAuthServiceExtension;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

/** */
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

  @Deprecated
  public static void main(String... args) throws Exception {
    new AuthTestDriver().getOAuthTokenCode();
  }

  /**
   * Performs an OAuth flow using the general AuthDataGenerator, returning a token.
   *
   * @return the token
   */
  public TokenAuthData getOAuthTokenCode() throws Exception {

    OkHttpClient client = TestHelper.createTestBuilder(callbackHost).build();

    AuthDataGenerator authDataGenerator = new MicrosoftAuthServiceExtension()
        .getAuthDataGenerator(DataVertical.CONTACTS, AuthMode.EXPORT);

    AuthFlowConfiguration configuration = authDataGenerator
        .generateConfiguration(callbackBase, "1");

    Desktop desktop = Desktop.getDesktop();

    desktop.browse(new URI(configuration.getAuthUrl()));

    // Execute the request and retrieve the auth code.
    String authCode = retrieveAuthCode(client);

    // get the token
    TokenAuthData tokenData = (TokenAuthData)
        authDataGenerator.generateAuthData(
            callbackBase, authCode, "1", configuration.getInitialAuthData(), null);

    // System.out.println("TOKEN: " + tokenData.getToken());
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

      // System.out.println("AUTH: " + authResponse.code() + ":" + authCode);

      return authCode;
    }
  }
}
