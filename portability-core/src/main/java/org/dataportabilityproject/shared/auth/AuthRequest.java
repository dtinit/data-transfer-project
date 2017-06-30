package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * Represents the url, and optional state, to request authorization for a service.
 */
@AutoValue
public abstract class AuthRequest {

  public static AuthRequest create(String url) {
    return create(url, null);
  }

  public static AuthRequest create(String url, @Nullable AuthData initialAuthData) {
    return new AutoValue_AuthRequest(url, initialAuthData);
  }

  public abstract String url();
  @Nullable public abstract AuthData initialAuthData();
}
