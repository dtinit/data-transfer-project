package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;

/**
 * A simple implimentation of {@link AuthData} that contains a token and a secret.
 */
@AutoValue
public abstract class TokenSecretAuthData extends AuthData {

  public static TokenSecretAuthData create(String token, String secret) {
    return new AutoValue_TokenSecretAuthData(token, secret);
  }

  public abstract String token();
  public abstract String secret();
}
