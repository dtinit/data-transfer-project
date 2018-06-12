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
package org.dataportabilityproject.api.action.datatype;

import com.google.inject.Inject;
import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.types.client.datatype.GetDataTypes;
import org.dataportabilityproject.types.client.datatype.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * An {@link Action} that handles listing data types available for export and import for a given
 * data type.
 */
public final class DataTypesAction
    implements Action<GetDataTypes, DataTypes> {
  private static final Logger logger = LoggerFactory.getLogger(DataTypesAction.class);
  private final AuthServiceProviderRegistry registry;

  @Inject
  DataTypesAction(AuthServiceProviderRegistry registry) {
    this.registry = registry;
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
      logger.warn("Empty data type list found");
    }
    return new DataTypes(transferDataTypes);
  }
}
