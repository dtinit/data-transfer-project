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
package org.dataportabilityproject.transfer.microsoft.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.spi.transfer.types.PaginationData;

/**
 * Encapsulates the Microsoft Graph API OData next link.
 *
 * FIXME: This needs to be registered with the system-wide TypeManager.
 */
@JsonTypeName("org.dataportability:GraphPagination")
public class GraphPagination extends PaginationData {
    private final String nextLink;

    @JsonCreator
    public GraphPagination(@JsonProperty("nextLink") String nextLink) {
        this.nextLink = nextLink;
    }

    public String getNextLink() {
        return nextLink;
    }
}
