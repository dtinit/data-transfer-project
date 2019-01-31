package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class BatchMediaItemResponse {
  @JsonProperty("newMediaItemResults")
  private NewMediaItemResult[] results;

  @JsonCreator
  public BatchMediaItemResponse(
      @JsonProperty("newMediaItemResults") NewMediaItemResult[] results) {
    this.results = results;
  }

  public NewMediaItemResult[] getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "BatchMediaItemResponse{" +
        "results=" + Arrays.toString(results) +
        '}';
  }
}
