package org.dataportabilityproject.shared;

import org.dataportabilityproject.dataModels.PaginationInformation;

public class StringPaginationToken  implements PaginationInformation {
  private final String id;

  public StringPaginationToken(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
