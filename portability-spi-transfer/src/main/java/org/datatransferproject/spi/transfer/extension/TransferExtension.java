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
package org.datatransferproject.spi.transfer.extension;

import org.datatransferproject.api.launcher.AbstractExtension;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AuthData;

/** Transfer extensions implement this contract to be loaded in a transfer worker process. */
public interface TransferExtension extends AbstractExtension {

  /** The key associated with this extension's service. */
  String getServiceId();

  default boolean supportsService(String service) {
    return this.getServiceId().toLowerCase().equals(service.toLowerCase());
  }

  /** Returns initialized extension exporter.
   * @param transferDataType*/
  Exporter<?, ?> getExporter(DataVertical transferDataType);

  /** Returns initialized extension importer.
   * @param transferDataType*/
  Importer<?, ?> getImporter(DataVertical transferDataType);

  /**
   * Returns initialized Signaller.
   */
  default SignalHandler<?> getSignalHandler() {
    return new SignalHandler<AuthData>(){};
  }
}
