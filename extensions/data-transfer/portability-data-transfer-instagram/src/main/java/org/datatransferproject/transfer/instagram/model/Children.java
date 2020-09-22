package org.datatransferproject.transfer.instagram.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class Children {
  @JsonProperty("data")
  private List<Child> data;

  public List<Child> getData() {
    return data;
  }
}
