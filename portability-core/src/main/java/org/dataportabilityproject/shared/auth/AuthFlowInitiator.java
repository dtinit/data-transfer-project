package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * Represents the authUrl, and optional state, to request authorization for a service.
 */
@AutoValue
public abstract class AuthFlowInitiator {

  public static AuthFlowInitiator create(String url) {
    return create(url, null);
  }

  public static AuthFlowInitiator create(String url, @Nullable AuthData initialAuthData) {
    return new AutoValue_AuthFlowInitiator(url, initialAuthData);
  }

  public abstract String authUrl();
  @Nullable public abstract AuthData initialAuthData();
}
