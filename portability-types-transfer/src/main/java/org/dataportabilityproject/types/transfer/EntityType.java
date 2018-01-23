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
package org.dataportabilityproject.types.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A uniquely identifiable entity in the system.
 */
public abstract class EntityType extends PortableType {
    @JsonProperty
    private String id;

    /**
     * Returns the unique identifier. A null value indicates the entity has not been persisted.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     */
    public void setId(String id) {
        this.id = id;
    }
}
