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
package org.datatransferproject.spi.transfer.provider;

import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.transfer.auth.AuthData;

import java.util.UUID;

/** Imports data into a destination service. */
public interface Importer<A extends AuthData, T extends DataModel> {
  /**
   * Imports data.
   *
   * @param jobId the ID for the job
   * @param idempotentExecutor an executor to wrap any import calls in
   * @param authData authentication information
   * @param data the data
   * @return the operation result
   */
  // We need to be able throw exceptions that can be caught by RetryingCallable.
  ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      A authData,
      T data)
      throws Exception;
}
