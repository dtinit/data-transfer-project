package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;

/**
 * A simple implementation of {@link AuthData} that contains a user name and password.
 */
@AutoValue
public abstract class PasswordAuthData extends AuthData {

  public static PasswordAuthData create(String username, String password) {
    return new AutoValue_PasswordAuthData(username, password);
  }

  public abstract String username();
  public abstract String password();
}
