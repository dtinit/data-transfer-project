/*
 * Copyright 2018 The Data Transfer Project Authors.
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** Creates a data transfer operation. */
@ApiModel(description = "A request to initiate a data transfer operation")
public class CreateTransfer {
  private String source; // REVIEW: corresponds to the import service
  private String destination; // REVIEW: corresponds to the export service
  // REVIEW: replace old PortableDataType since the latter is an enum and not extensible?
  private String transferDataType;

  @JsonCreator
  public CreateTransfer(
      @JsonProperty(value = "source", required = true) String source,
      @JsonProperty(value = "destination", required = true) String destination,
      @JsonProperty(value = "transferDataType", required = true) String transferDataType) {
    this.source = source;
    this.destination = destination;
    this.transferDataType = transferDataType;
  }

  @ApiModelProperty(
    value = "The service to transfer data from",
    dataType = "string",
    required = true
  )
  public String getSource() {
    return source;
  }

  @ApiModelProperty(value = "The service to transfer data to", dataType = "string", required = true)
  public String getDestination() {
    return destination;
  }

  @ApiModelProperty(value = "The type of data to transfer", dataType = "string", required = true)
  public String getTransferDataType() {
    return transferDataType;
  }
}
