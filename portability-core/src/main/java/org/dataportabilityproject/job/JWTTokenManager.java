/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.job;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for token creation and verification.
 */
public class JWTTokenManager implements TokenManager {

  public static final String JWT_KEY_NAME = "JWT_KEY";
  public static final String JWT_SECRET_NAME = "JWT_SECRET";

  // TODO: determine proper issuer for JWT tokens
  private static final String ISSUER = "dataportabilityproject";
  // Key for the portability id stored as a private 'claim' in the JWT
  private static final String ID_CLAIM_KEY = "portability-id";
  private static final int EXPIRATION_TIME_MILLIS = 1000 * 60; // 1 minute expiration

  private final Algorithm algorithm;
  private final Logger logger = LoggerFactory.getLogger(JWTTokenManager.class);
  private JWTVerifier verifier;

  public JWTTokenManager(String secret) {
    this.algorithm = createAlgorithm(secret);
    this.verifier = createVerifier(secret, ISSUER);
  }

  /**
   * Create an instance of the token verifier.
   */
  private static JWTVerifier createVerifier(String secret, String issuer) {
    return JWT.require(createAlgorithm(secret))
        .withIssuer(issuer)
        .build();
  }

  /**
   * Create the {@link Algorithm} to be used for signing and parsing tokens.
   */
  private static Algorithm createAlgorithm(String secret) {
    try {
      return Algorithm.HMAC256(secret);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); // TODO: Better error handling
    }
  }

  @Override
  public boolean verifyToken(String token) {
    try {
      DecodedJWT jwt = verifier.verify(token);
      return true;
    } catch (JWTVerificationException exception) {
      logger.debug("Error verifying token: {}", exception);
      logger.debug("Token: {}", token);
      return false;
    }
  }

  @Override
  public String getData(String token) {
    try {
      DecodedJWT jwt = verifier.verify(token);
      // Token is verified, get claim
      Claim claim = jwt.getClaim(JWTTokenManager.ID_CLAIM_KEY);
      if (claim.isNull()) {
        return null;
      }
      return claim.isNull() ? null : claim.asString();
    } catch (JWTVerificationException exception) {

      throw new RuntimeException("Error verifying token: " + token);
    }
  }

  @Override
  public String createNewToken(String uuid) {
    try {
      return JWT.create()
          .withIssuer(JWTTokenManager.ISSUER)
          .withClaim(JWTTokenManager.ID_CLAIM_KEY, uuid)
          .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MILLIS))
          .sign(algorithm);
    } catch (JWTCreationException e) {
      throw new RuntimeException("Error creating token for: " + uuid);
    }
  }
}
