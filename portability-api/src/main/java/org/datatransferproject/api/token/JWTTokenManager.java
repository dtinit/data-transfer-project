/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.api.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.token.TokenManager;

import java.lang.IllegalArgumentException;
import java.util.Date;
import java.util.UUID;

/** Utility methods for token creation and verification. */
public class JWTTokenManager implements TokenManager {

  public static final String JWT_KEY_NAME = "JWT_KEY";
  public static final String JWT_SECRET_NAME = "JWT_SECRET";

  // TODO: determine proper issuer for JWT tokens
  private static final String ISSUER = "datatransferproject";
  // Key for the portability id stored as a private 'claim' in the JWT
  private static final String ID_CLAIM_KEY = "portability-id";
  private static final int EXPIRATION_TIME_MILLIS = 1000 * 60; // 1 minute expiration

  private final Algorithm algorithm;
  private final JWTVerifier verifier;
  private final Monitor monitor;

  @Inject
  public JWTTokenManager(String secret, Monitor monitor) {
    this.algorithm = createAlgorithm(secret);
    this.verifier = createVerifier(secret, ISSUER);
    this.monitor = monitor;
  }

  /** Create an instance of the token verifier. */
  private static JWTVerifier createVerifier(String secret, String issuer) {
    return JWT.require(createAlgorithm(secret)).withIssuer(issuer).build();
  }

  /** Create the {@link Algorithm} to be used for signing and parsing tokens. */
  private static Algorithm createAlgorithm(String secret) {
    try {
      return Algorithm.HMAC256(secret);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e); // TODO: Better error handling
    }
  }

  @Override
  public boolean verifyToken(String token) {
    try {
      verifier.verify(token);
      return true;
    } catch (JWTVerificationException exception) {
      monitor.debug(() -> "Error verifying token", exception);
      return false;
    }
  }

  @Override
  public UUID getJobIdFromToken(String token) {
    try {
      DecodedJWT jwt = verifier.verify(token);
      // Token is verified, get claim
      Claim claim = jwt.getClaim(JWTTokenManager.ID_CLAIM_KEY);
      if (claim.isNull()) {
        return null;
      }
      return claim.isNull() ? null : UUID.fromString(claim.asString());
    } catch (JWTVerificationException exception) {
      monitor.debug(() -> "Error verifying token", exception);
      throw new RuntimeException("Error verifying token: " + token);
    }
  }

  @Override
  public String createNewToken(UUID jobId) {
    try {
      return JWT.create()
          .withIssuer(JWTTokenManager.ISSUER)
          .withClaim(JWTTokenManager.ID_CLAIM_KEY, jobId.toString())
          .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MILLIS))
          .sign(algorithm);
    } catch (JWTCreationException e) {
      throw new RuntimeException("Error creating token for: " + jobId);
    }
  }
}
