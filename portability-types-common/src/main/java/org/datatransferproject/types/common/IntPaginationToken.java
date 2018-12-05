package org.datatransferproject.types.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.PaginationData;

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
