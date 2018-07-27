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

/** A data transfer job. */
public class TransferJob {
    public enum State {
        CREATED,
        SUBMITTED,
        IN_PROGRESS,
        COMPLETE
    }

    private final String id;
    private final String source;
    private final String destination;
    private final String dataType;
    private final State state = State.CREATED;
    private final String exportUrl;
    private final String importUrl;

    @JsonCreator
    public TransferJob(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "dataType", required = true) String dataType,
            @JsonProperty(value = "exportUrl", required = true) String exportUrl,
            @JsonProperty(value = "importUrl", required = true) String importUrl) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.dataType = dataType;
        this.exportUrl = exportUrl;
        this.importUrl = importUrl;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getDataType() {
        return dataType;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public String getImportUrl() {
        return importUrl;
    }
}
