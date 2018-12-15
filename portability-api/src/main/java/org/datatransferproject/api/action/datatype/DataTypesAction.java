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
package org.datatransferproject.api.action.datatype;

import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.types.client.datatype.DataTypes;
import org.datatransferproject.types.client.datatype.GetDataTypes;

import java.util.Set;

/**
 * An {@link Action} that handles listing data types available for export and import for a given
 * data type.
 */
public final class DataTypesAction implements Action<GetDataTypes, DataTypes> {
  private final AuthServiceProviderRegistry registry;
  private final Monitor monitor;

  @Inject
  DataTypesAction(AuthServiceProviderRegistry registry, Monitor monitor) {
    this.registry = registry;
    this.monitor = monitor;
  }

  @Override
  public Class<GetDataTypes> getRequestType() {
    return GetDataTypes.class;
  }

  /** Lists the set of data types that support both import and export. */
  @Override
  public DataTypes handle(GetDataTypes request) {
    Set<String> transferDataTypes = registry.getTransferDataTypes();
    if (transferDataTypes.isEmpty()) {
      monitor.severe(
          () ->
              "No transfer data types were registered in "
                  + AuthServiceProviderRegistry.class.getName());
    }
    return new DataTypes(transferDataTypes);
  }
}
