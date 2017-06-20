package org.dataportabilityproject.shared.auth;

import java.io.IOException;

/** Methods to generate AuthData for online */
public interface OnlineAuthDataGenerator {
  /** Provide auth url. In the Oauth2 case, this is the authorization code url. */
  String generateAuthUrl(String id) throws IOException;

  /** Store auth data. */
  public AuthData generateAuthData(String authCode, String id) throws IOException;
}
