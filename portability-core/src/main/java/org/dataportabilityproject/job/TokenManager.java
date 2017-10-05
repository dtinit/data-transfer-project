package org.dataportabilityproject.job;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

/** Functionality to manage the lifecycle of tokens. */
public interface TokenManager {

  /** Verifies if the token is valid. */
  boolean verifyToken(String token);

  /** Verifies and returns the data associated with this token. */
  String getData(String token);

  /** Creates a new JWT token with the given {@code uuid}. */
  String createNewToken(String uuid);
}
