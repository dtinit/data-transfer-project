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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** Request to create a transfer job. */
@ApiModel(description = "A request to create a data transfer job")
public class CreateTransferJob {
    private final String source;
    private final String destination;
    private final String dataType;
    private final String baseCallbackUrl;

    @JsonCreator
    public CreateTransferJob(
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "dataType", required = true) String dataType,
            @JsonProperty(value = "baseCallbackUrl", required = true) String baseCallbackUrl) {
        this.source = source;
        this.destination = destination;
        this.dataType = dataType;
        this.baseCallbackUrl = baseCallbackUrl;
    }

    @ApiModelProperty(value = "The service to transfer data from", dataType = "string", required = true)
    public String getSource() {
        return source;
    }

    @ApiModelProperty(value = "The service to transfer data to", dataType = "string", required = true)
    public String getDestination() {
        return destination;
    }

    @ApiModelProperty(value = "The type of data to transfer", dataType = "string", required = true)
    public String getDataType() {
        return dataType;
    }

    @ApiModelProperty(value = "The base of the auth callback URLs", dataType = "string", required = true)
    public String getBaseCallbackUrl() {
        return baseCallbackUrl;
    }
}
