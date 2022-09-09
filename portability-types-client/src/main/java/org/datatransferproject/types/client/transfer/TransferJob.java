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
import org.datatransferproject.types.common.models.DataVertical;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol;

/**
 * A data transfer job.
 *
 * <p>TODO(#553): Refactor String params to make less error-prone.
 */
public class TransferJob {
    public enum State {
        CREATED,
        SUBMITTED,
        IN_PROGRESS,
        COMPLETE
    }

    private final String id;
    private final String exportService;
    private final String importService;
    private final DataVertical dataType;
    private final State state = State.CREATED;
    private final String exportUrl;
    private final String importUrl;
    private final String exportTokenUrl;
    private final String importTokenUrl;
    private final AuthProtocol exportAuthProtocol;
    private final AuthProtocol importAuthProtocol;

    @JsonCreator
    public TransferJob(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "exportService", required = true) String exportService,
            @JsonProperty(value = "importService", required = true) String importService,
            @JsonProperty(value = "dataType", required = true) DataVertical dataType,
            @JsonProperty(value = "exportUrl", required = true) String exportUrl,
            @JsonProperty(value = "importUrl", required = true) String importUrl,
            @JsonProperty(value = "exportTokenUrl", required = true) String exportTokenUrl,
            @JsonProperty(value = "importTokenUrl", required = true) String importTokenUrl,
            @JsonProperty(value = "exportAuthProtocol", required = true) AuthProtocol exportAuthProtocol,
            @JsonProperty(value = "importAuthProtocol", required = true) AuthProtocol importAuthProtocol) {
        this.id = id;
        this.exportService = exportService;
        this.importService = importService;
        this.dataType = dataType;
        this.exportUrl = exportUrl;
        this.importUrl = importUrl;
        this.exportTokenUrl = exportTokenUrl;
        this.importTokenUrl = importTokenUrl;
        this.exportAuthProtocol = exportAuthProtocol;
        this.importAuthProtocol = importAuthProtocol;
    }

    public String getId() {
        return id;
    }

    public String getExportService() {
        return exportService;
    }

    public String getImportService() {
        return importService;
    }

    public DataVertical getDataType() {
        return dataType;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public String getImportUrl() {
        return importUrl;
    }

    public String getExportTokenUrl() {
        return exportTokenUrl;
    }

    public String getImportTokenUrl() {
        return importTokenUrl;
    }

    public AuthProtocol getExportAuthProtocol() {
        return exportAuthProtocol;
    }

    public AuthProtocol getImportAuthProtocol() {
        return importAuthProtocol;
    }
}
