package org.dataportabilityproject.spi.gateway.auth;

import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AuthData;

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
}
