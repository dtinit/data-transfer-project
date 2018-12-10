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
package org.datatransferproject.spi.transfer.hooks;

import java.util.UUID;

/** A default implementation of the job hooks. */
public interface JobHooks {

  /** Called when a job starts processing on a worker. */
  default void jobStarted(UUID jobId) {}

  /**
   * Called when a job finishes processing on a worker. The {@code success} parameter indicates
   * whether the transfer was succesful or not.
   */
  default void jobFinished(UUID jobId, boolean success) {}
}
