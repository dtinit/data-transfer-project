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

import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 * Exporters produce data from some source/origin service.
 *
 * <p>Exporter classes are typically the concrete meeting point between DTP framework and a third
 * party API, but specifically: when DTP needs to <bold>read</bold> data from a service, its an
 * Exporter that does that reading. This is in contrast to {@link Importer}s which write that same
 * data.
 *
 * <h3>API for `Export#export` calls
 *
 * <p>DTP copiers can will call `export` in three key ways:
 *
 * <ol>
 *   <li>to start a new transfer job for a user's whole library, indicated by an Optional.empty()
 *       for ExportInformation; expected return:
 *       <ul>
 *         <li>a resource container for anything that's ready for export.
 *         <li>a continuation data for any new resources discovered (aka "sub" resources).
 *         <li>a ResultType of END if no further continuation data is passed. (this should coincide
 *             with the absence of continuation data; if this isn't true, then DTP's behavior is
 *             undefined)
 *       </ul>
 *   <li>to continue paging through a listing of items (aka "sub" resources), as indicated by one's
 *       own previous return of a ContinuationData.
 *   <li>to start a new transfer job for a subset of whole library, indicated by a null for
 *       Optional<ExportInformation> (not to be confused with the first case above); expected
 *       return: contract as first case
 * </ol>
 *
 * <p>The recursive nature of this API--that one's own returns are partially used for one's own
 * subsequent calls--means each Exporter is free to design the exact types and contents of those
 * objects freely, without interference. That means to make an exporter maintainable, an exporter
 * **must** carefully document the above cases, but replace the high-level description with the
 * concrete types and what they indicate for the particular libraries the exporter is designed for.
 *
 * <h3>Stateful APIs
 *
 * <p>A word on the implied contract of DTP imports and exporters (aka adapters). DTP adapters have
 * two states to track:
 *
 * <ol>
 *   <li>construction-time state when DTP itself starts and the owning extension constructs its
 *       class
 *   <li>per-transfer-job state when DTP calls the exporter's `export` method to start a new job
 * </ol>
 *
 * </lp>
 *
 * <p>The former state is infrastructure-sensitive (like what JobStore backend is available), where
 * the latter's state is user-sensitive. This means the design of DTP adapters, largely copy/pasted
 * patterns out of a the same small seed of adapters, is constantly trying to protect against a
 * hypothetical future where where a DTP server could concurrently ask the same Exporter to handle
 * distinct transfer jobs (currently handle distinct `export` calls).
 *
 * <p>However this can be misleading and confusing: some classes uses a map, some classes refuse to
 * store any user-sensitive state on the instance altogether and just have **all** their internal
 * methods repeat the same parameters to pass around. Some have a mix where of repeating
 * parameter-passing **and** try to carefully use `volatile` and `synchronized` keyword to store
 * data on their own instance.
 */
public interface Exporter<A extends AuthData, T extends DataModel> {
  // TODO: reconsider this model - can we avoid sending AuthData with every
  // export call? Example: given the two state-management modes described in
  // javadoc above, we could have an `Exporter` isntance for the infrastructure
  // state, and an `JobExport` instance (that said Exporter constructs) that
  // contains all the per-transfer state like AuthData and JobId; in such a
  // case the new API on this interface would be `getJobExporter(jobId,
  // authData)` that does the construction for us, and we call export() on
  // _that_ instead (with _only_ ExportInformation as the input paramter).

  /**
   * Performs an export operation, starting from the data specified by the continuation.
   *
   * @param jobId the job id
   * @param authData authentication data for the operation
   * @param exportInformation info about what data to export see {@link ExportInformation} for more
   */
  // We need to be able throw exceptions that can be caught by RetryingCallable.
  ExportResult<T> export(UUID jobId, A authData, Optional<ExportInformation> exportInformation)
      throws Exception;
}
