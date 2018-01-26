package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class ListDataTypesResponse {

  private final String[] dataTypes;

  @JsonCreator
  public ListDataTypesResponse(
      @JsonProperty(value = "dataTypes", required = true) String[] dataTypes) {
    this.dataTypes = dataTypes;
  }

  @ApiModelProperty
  public String[] getDataTypes() {
    return this.dataTypes;
  }
}
