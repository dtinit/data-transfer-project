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

/**
 * An extension that contributes a set of capabilities to a system.
 *
 * <p>Extensions fall into two categories: those required to boot a runtime system where basic
 * services have not yet been initialized; and those that are instantiated after the core boot
 * process has completed and core services are available.
 */
public interface SystemExtension {
  /**
   * Signals to the extension to prepare for receiving requests. For example, implementations may
   * open sockets or other resources.
   */
  default void start() {}

  /** Signals the extension to terminate and cleanup resources. */
  default void shutdown() {}
}
