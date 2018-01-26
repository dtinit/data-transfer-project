package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class ListServicesResponse {

  private final String transferDataType;
  private final String[] exportServices;
  private final String[] importServices;

  @JsonCreator
  public ListServicesResponse(
      @JsonProperty(value = "transferDataType", required = true) String transferDataType,
      @JsonProperty(value = "exportServices", required = true) String[] exportServices,
      @JsonProperty(value = "importServices", required = true) String[] importServices) {
    this.transferDataType = transferDataType;
    this.importServices = importServices;
    this.exportServices = exportServices;
  }

  @ApiModelProperty
  public String[] getImportServices() {
    return this.importServices;
  }

  @ApiModelProperty
  public String[] getExportServices() {
    return this.exportServices;
  }

  @ApiModelProperty
  public String getTransferDataType() {
    return this.transferDataType;
  }
}
