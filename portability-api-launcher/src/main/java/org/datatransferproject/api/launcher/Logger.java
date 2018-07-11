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
 * The system logger used to record events. Implementations may delegate to a logging
 * implementation.
 */
public interface Logger {

  /**
   * Records a severe (error) level event.
   *
   * @param supplier the event message
   * @param errors optional raised errors
   */
  default void severe(Supplier<String> supplier, Throwable... errors) {}

  /**
   * Records a info level event.
   *
   * @param supplier the event message
   * @param errors optional raised errors
   */
  default void info(Supplier<String> supplier, Throwable... errors) {}

  /**
   * Records a debug level event.
   *
   * @param supplier the event message
   * @param errors optional raised errors
   */
  default void debug(Supplier<String> supplier, Throwable... errors) {}
}
