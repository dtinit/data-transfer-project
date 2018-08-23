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
    private final String exportService;
    private final String importService;
    private final String exportCallbackUrl;
    private final String importCallbackUrl;
    private final String dataType;

    @JsonCreator
    public CreateTransferJob(
            @JsonProperty(value = "exportService", required = true) String exportService,
            @JsonProperty(value = "importService", required = true) String importService,
            @JsonProperty(value = "exportCallbackUrl", required = true) String exportCallbackUrl,
            @JsonProperty(value = "importCallbackUrl", required = true) String importCallbackUrl,
            @JsonProperty(value = "dataType", required = true) String dataType) {
        this.exportService = exportService;
        this.importService = importService;
        this.exportCallbackUrl = exportCallbackUrl;
        this.importCallbackUrl = importCallbackUrl;
        this.dataType = dataType;
    }

    @ApiModelProperty(value = "The service to transfer data from", dataType = "string", required = true)
    public String getExportService() {
        return exportService;
    }

    @ApiModelProperty(value = "The service to transfer data to", dataType = "string", required = true)
    public String getImportService() {
        return importService;
    }

    @ApiModelProperty(value = "The export auth callback URL", dataType = "string", required = true)
    public String getExportCallbackUrl() {
        return exportCallbackUrl;
    }

    @ApiModelProperty(value = "The import auth callback URL", dataType = "string", required = true)
    public String getImportCallbackUrl() {
        return importCallbackUrl;
    }

    @ApiModelProperty(value = "The type of data to transfer", dataType = "string", required = true)
    public String getDataType() {
        return dataType;
    }
}
