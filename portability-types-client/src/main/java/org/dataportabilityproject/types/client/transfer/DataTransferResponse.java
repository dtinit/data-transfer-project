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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A transfer operation in the system.
 */
@ApiModel(description = "A transfer operation in the system")
public class DataTransferResponse extends AbstractDataTransfer {

    @ApiModel
    public enum Status {
        INPROCESS, COMPLETE, ERROR
    }

    private Status status;

    // The URL to go to after this is returned from the API
    private String nextUrl;


    @JsonCreator
    public DataTransferResponse(
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "transferDataType", required = true) String transferDataType,
            @JsonProperty(value = "status", required = true) Status status,
            @JsonProperty(value = "nextUrl") String nextUrl) {
        super(source, destination, transferDataType);
        this.status = status;
        this.nextUrl = nextUrl;
    }

    @ApiModelProperty
    public Status getStatus() {
        return status;
    }

    @ApiModelProperty
    public String getNextUrl() { return nextUrl;}
}
