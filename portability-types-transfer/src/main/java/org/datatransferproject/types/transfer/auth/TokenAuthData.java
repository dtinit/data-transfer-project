package org.datatransferproject.types.transfer.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/** Token-based authentication data. */
@JsonTypeName("org.dataportability:TokenAuthData")
public class TokenAuthData extends AuthData {
  private final String token;

  @JsonCreator
  public TokenAuthData(@JsonProperty("token") String token) {
    this.token = token;
  }

  @Override
  public String getToken() {
    return token;
  }
}
