package org.dataportabilityproject.serviceProviders.smugmug.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class SmugMugPageInfo {
  @JsonProperty("Total")
  private int total;

  @JsonProperty("Start")
  private int start;

  @JsonProperty("Count")
  private int count;

  @JsonProperty("RequestedCount")
  private int requestedCount;

  @JsonProperty("NextPage")
  private String nextPage;

  public String getNextPage() {
    return nextPage;
  }
}
