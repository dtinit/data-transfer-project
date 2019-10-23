package org.datatransferproject.types.transfer.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/** Token-secret-based authentication data. */
@JsonTypeName("org.dataportability:TokenSecretAuthData")
public class TokenSecretAuthData extends AuthData {
  private final String token;
  private final String secret;

  @JsonCreator
  public TokenSecretAuthData(
      @JsonProperty("token") String token, @JsonProperty("secret") String secret) {
    this.token = token;
    this.secret = secret;
  }

  @Override
  public String getToken() {
    return token;
  }

  public String getSecret() {
    return secret;
  }
}
