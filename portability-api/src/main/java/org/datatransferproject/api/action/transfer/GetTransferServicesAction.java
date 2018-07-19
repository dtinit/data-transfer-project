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
package org.datatransferproject.api.action.transfer;

import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.action.ActionUtils;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.types.client.transfer.TransferServices;
import org.datatransferproject.types.client.transfer.GetTransferServices;

import java.util.Set;

/** Returns the import and export services available for a given data type. */
public final class GetTransferServicesAction
    implements Action<GetTransferServices, TransferServices> {

  private final AuthServiceProviderRegistry registry;

  @Inject
  GetTransferServicesAction(AuthServiceProviderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Class<GetTransferServices> getRequestType() {
    return GetTransferServices.class;
  }

  /** Lists the services available for export and import for a given data type. */
  @Override
  public TransferServices handle(GetTransferServices request) {
    String transferDataType = request.getTransferDataType();
    // Validate incoming data type parameter
    if (!ActionUtils.isValidTransferDataType(transferDataType)) {
      throw new IllegalArgumentException("Invalid transferDataType: " + transferDataType);
    }

    Set<String> importServices = registry.getImportServices(transferDataType);
    Set<String> exportServices = registry.getExportServices(transferDataType);

    if (importServices.isEmpty() || exportServices.isEmpty()) {
      throw new IllegalArgumentException(
          "[" + transferDataType + "] does not have an import and export service");
    }
    return new TransferServices(transferDataType, exportServices,importServices);
  }
}
