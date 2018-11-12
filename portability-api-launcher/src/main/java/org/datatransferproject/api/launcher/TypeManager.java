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
package org.datatransferproject.api.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Manages known JSON types and databinding. */
public interface TypeManager {

  /**
   * Returns the system-wide {@code ObjectMapper} that is configured to databind built-in and
   * extension model types in the system.
   */
  ObjectMapper getMapper();

  /**
   * Registers a model type. Extensions that introduce new model subtypes must registered them here
   * so they can be databound properly.
   *
   * @param type the type to register.
   */
  void registerType(Class<?> type);

  /**
   * Registers a model type. Extensions that introduce new model subtypes must registered them here
   * so they can be databound properly.
   *
   * @param types the type to register.
   */
  void registerTypes(Class<?>... types);
}
