package org.datatransferproject.spi.api.auth;

import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 * Generates data for authentication flows.
 *
 * <p>REVIEW: removed IOException
 */
public interface AuthDataGenerator {

  /**
   * Provide a authUrl to redirect the user to authenticate. In the Oauth2 case, this is the
   * authorization code authUrl.
   *
   * @param callbackBaseUrl the url to the api server serving the callback for the auth request
   * @param id is a client supplied identifier
   */
  AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id);

  /**
   * Generate auth data given the code, identifier, and, optional, initial auth data that was used
   * for earlier steps of the authentication flow.
   *
   * @param callbackBaseUrl the url to the api server serving the callback for the auth request
   * @param authCode The authorization code or oauth verififer after user authorization
   * @param id is a client supplied identifier
   * @param initialAuthData optional data resulting from the initial auth step
   * @param extra optional additional code, password, etc.
   */
  AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra);

  /**
   * Return the URL used to exchange an access code for token.
   *
   * <p>TODO(#553): Implement this for auth extensions. Currently unused by the demo-server frontend.
   */
  default String getTokenUrl() {
    return "";
  }
}
