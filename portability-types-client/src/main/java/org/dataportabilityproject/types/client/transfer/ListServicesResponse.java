/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
