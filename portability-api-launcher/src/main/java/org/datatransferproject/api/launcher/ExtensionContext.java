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

import org.datatransferproject.api.launcher.Constants.Environment;

/** Provides information required to bootstrap extensions. */
public interface ExtensionContext {

  /**
   * Returns the runtime type manager.
   *
   * @return the type manager.
   */
  TypeManager getTypeManager();

  /**
   * Returns the system monitor.
   *
   * @return the monitor
   */
  Monitor getMonitor();

  /**
   * Returns a system service such as a type mapper extension services may require.
   *
   * @param type the system service type
   */
  <T> T getService(Class<T> type);

  /**
   * Registers a service
   *
   * @param type the type to register the service for
   * @param service the service instance
   */
  default <T> void registerService(Class<T> type, T service) {}

  /**
   * Returns the configuration value for an extension setting, or the default value if not found.
   *
   * @param setting the parameter key
   * @param defaultValue the default value. Null may be passed.
   */
  <T> T getSetting(String setting, T defaultValue);

  // Required settings
  @Flag
  String cloud();

  @Flag
  Environment environment();
}
