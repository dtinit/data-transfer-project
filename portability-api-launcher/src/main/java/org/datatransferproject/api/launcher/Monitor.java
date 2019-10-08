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

import java.util.function.Supplier;

/**
 * The system monitoring and logging interface which can be obtained either via injection or from
 * {@link ExtensionContext#getMonitor()}.
 *
 * <p>Implementations are responsible for processing and forwarding events to an external sink or
 * service.
 */
public interface Monitor {

  /**
   * Records a severe (error) level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void severe(Supplier<String> supplier, Object... data) {}

  /**
   * Records a info level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void info(Supplier<String> supplier, Object... data) {}

  /**
   * Records a debug level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void debug(Supplier<String> supplier, Object... data) {}

  /**
   * Makes sure any queued logs are sent. Recommended to be called before exiting the Java process.
   */
  default void flushLogs() {}
}
