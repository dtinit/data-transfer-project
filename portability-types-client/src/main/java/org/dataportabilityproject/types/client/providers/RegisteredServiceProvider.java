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
package org.dataportabilityproject.types.client.providers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** A service provider registered in the system. */
@ApiModel(description = "A service provider registered in the system")
public class RegisteredServiceProvider {
  private String id;
  private String name;
  private String description;

  private String[] transferDataTypes;

  @JsonCreator
  public RegisteredServiceProvider(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "name", required = true) String name,
      @JsonProperty(value = "description", required = true) String description,
      @JsonProperty(value = "transferDataTypes", required = true) String[] transferDataTypes) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.transferDataTypes = transferDataTypes;
  }

  @ApiModelProperty(value = "The unique service provider id")
  public String getId() {
    return id;
  }

  @ApiModelProperty(value = "The service provider name")
  public String getName() {
    return name;
  }

  @ApiModelProperty(value = "The service provider description")
  public String getDescription() {
    return description;
  }

  @ApiModelProperty(value = "The data types types supported by this service provider")
  public String[] getTransferDataTypes() {
    return transferDataTypes;
  }
}
