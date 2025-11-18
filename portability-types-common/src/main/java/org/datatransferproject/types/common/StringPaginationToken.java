package org.datatransferproject.types.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * String pagination data.
 */
@JsonTypeName("org.dataportability:StringPagination")
public class StringPaginationToken extends PaginationData {
  private final String token;

  /**
   * Ctor.
   *
   * @param token the token to get the next page
   */
  @JsonCreator
  public StringPaginationToken(@JsonProperty("token") String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void verify(String prefix){
    Preconditions.checkArgument(
        getToken().startsWith(prefix), "Invalid pagination token %s",
        getToken());
  }

  public Optional<String> getParsedToken(String prefix){
      Optional<String> parsedToken = Optional.empty();
      if (prefix.length() < token.length()) {
        parsedToken = Optional.of(token.substring(prefix.length()));
      }
    return parsedToken;
  }
}
