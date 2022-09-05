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

package org.datatransferproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;
import org.datatransferproject.types.common.models.DataVertical;

/** Export and import services that support the provided data type. */
public class TransferServices {
  private final DataVertical transferDataType;
  private final Set<String> exportServices;
  private final Set<String> importServices;

  @JsonCreator
  public TransferServices(
      @JsonProperty(value = "transferDataType", required = true) DataVertical transferDataType,
      @JsonProperty(value = "exportServices", required = true) Set<String> exportServices,
      @JsonProperty(value = "importServices", required = true) Set<String> importServices) {
    this.transferDataType = transferDataType;
    this.importServices = importServices;
    this.exportServices = exportServices;
  }

  @ApiModelProperty
  public Set<String> getImportServices() {
    return this.importServices;
  }

  @ApiModelProperty
  public Set<String> getExportServices() {
    return this.exportServices;
  }

  @ApiModelProperty
  public DataVertical getTransferDataType() {
    return this.transferDataType;
  }
}
