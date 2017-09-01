package org.dataportabilityproject.shared;

import com.google.common.base.MoreObjects;
import org.dataportabilityproject.dataModels.PaginationInformation;

public class IntPaginationToken implements PaginationInformation {
  // The number the next page starts at
  private final int start;

  public IntPaginationToken(int start) {
    this.start = start;
  }

  public int getStart() {
    return start;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("start", start)
        .toString();
  }
}
