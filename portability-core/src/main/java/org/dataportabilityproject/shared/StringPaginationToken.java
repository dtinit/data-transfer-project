package org.dataportabilityproject.shared;

import com.google.common.base.MoreObjects;
import org.dataportabilityproject.dataModels.PaginationInformation;

public class StringPaginationToken  implements PaginationInformation {
  private final String id;

  public StringPaginationToken(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .toString();
  }
}
