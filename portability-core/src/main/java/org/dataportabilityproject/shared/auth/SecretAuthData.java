package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;

/**
 * A simple implimentation of {@link AuthData} that contains just a secret.
 */
@AutoValue
public abstract class SecretAuthData extends AuthData {

  public static SecretAuthData create(String secret) {
    return new AutoValue_SecretAuthData(secret);
  }

  public abstract String secret();
}
