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
package org.dataportabilityproject.spi.transfer.provider;

import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

/** Exports data from a source service. */
public interface Exporter<A extends AuthData, T extends DataModel> {
  /** Performs an export operation. */
  ExportResult<T> export(A authData);

  /**
   * Performs an export operation, starting from the data specified by the continuation.
   *
   * @param authData authentication data for the operation
   * @param exportInformation info about what data to export see {@link ExportInformation} for more
   *     info
   */
  ExportResult<T> export(
      A authData,
      ExportInformation
          exportInformation); // REVIEW: The original throws IOException. Continue to use checked
                              // exceptions or use unchecked?
}
