/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.gateway.action.listdatatypes;

import com.google.inject.Inject;
import java.util.Set;
import org.dataportabilityproject.gateway.action.Action;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Action} that handles listing data types available for export and import for a given
 * data type.
 */
public final class ListDataTypesAction implements
    Action<ListDataTypesActionRequest, ListDataTypesActionResponse> {
  private static final Logger logger = LoggerFactory.getLogger(
      ListDataTypesAction.class);
  private final AuthServiceProviderRegistry registry;


  @Inject
  ListDataTypesAction(
      AuthServiceProviderRegistry registry
  ) {
    this.registry = registry;
  }

  /**
   * Lists the set of data types that support both import and export.
   */
  @Override
  public ListDataTypesActionResponse handle(ListDataTypesActionRequest request) {
    Set<String> transferDataTypes = registry.getTransferDataTypes();
    if (transferDataTypes.isEmpty()) {
      logger.warn("Empty data type list found");
    }
    return ListDataTypesActionResponse.create(transferDataTypes);
  }
}
