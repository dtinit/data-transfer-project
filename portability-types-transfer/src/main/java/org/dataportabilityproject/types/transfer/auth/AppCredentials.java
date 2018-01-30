package org.dataportabilityproject.types.transfer.auth;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class AppCredentials {
  private final String key;
  private final String secret;

  @JsonCreator
  public AppCredentials(String key, String secret) {
    this.key = key;
    this.secret = secret;
  }

  public String getKey() {
    return key;
  }

  public String getSecret() {
    return secret;
  }
}
