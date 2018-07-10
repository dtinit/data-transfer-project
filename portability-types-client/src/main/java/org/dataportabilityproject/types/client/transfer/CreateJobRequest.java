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

/** Request to create a data transfer job. */
@ApiModel(description = "Request to create a data transfer job")
public class CreateJobRequest extends AbstractDataTransfer {

  @JsonCreator
  public CreateJobRequest(
      @JsonProperty(value = "source", required = true) String source,
      @JsonProperty(value = "destination", required = true) String destination,
      @JsonProperty(value = "transferDataType", required = true) String transferDataType) {
    super(source, destination, transferDataType);
  }
}
