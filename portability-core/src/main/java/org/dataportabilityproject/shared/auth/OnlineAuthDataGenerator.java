package org.dataportabilityproject.shared.auth;

import java.io.IOException;

/** Methods to generate AuthData for online */
public interface OnlineAuthDataGenerator {
  /** Provide auth url. In the Oauth2 case, this is the authorization code url. */
  AuthRequest generateAuthUrl(String id) throws IOException;

  /** Generate auth data given the code, identifier, and, optional, initial auth data that was
   * used for earlier steps of the authentication flow.
   * @param authCode The authorization code or oauth verififer after user authorization
   * @param id is a client supplied identifier
   * @param initialAuthData optional data resulting from the initial auth step
   */
  public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData) throws IOException;
}
