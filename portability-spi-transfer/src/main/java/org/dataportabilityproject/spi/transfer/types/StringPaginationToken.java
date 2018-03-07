package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

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
}
