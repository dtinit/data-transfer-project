package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/** Numeric pagination data. */
@JsonTypeName("org.dataportability:NumberPagination")
public class IntPaginationToken extends PaginationData {
  private final int start;

  /**
   * Ctor.
   *
   * @param start the number the next page starts on.
   */
  @JsonCreator
  public IntPaginationToken(@JsonProperty("start") int start) {
    this.start = start;
  }

  public int getStart() {
    return start;
  }
}
