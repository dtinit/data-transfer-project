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
package org.datatransferproject.launcher.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.datatransferproject.api.launcher.TypeManager;

/** Jackson-based type manager that supports polymorphic type handling. */
public class TypeManagerImpl implements TypeManager {
  private final ObjectMapper objectMapper;

  public TypeManagerImpl() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(
        new JavaTimeModule()); // configure ISO 8601 time de/serialization support
  }

  public ObjectMapper getMapper() {
    return objectMapper;
  }

  public void registerType(Class<?> type) {
    objectMapper.registerSubtypes(type);
  }

  public void registerTypes(Class<?>... types) {
    for (Class<?> t : types) {
      objectMapper.registerSubtypes(t);
    }
  }
}
