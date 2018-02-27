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
package org.dataportabilityproject.gateway.action.listservices;

import com.google.inject.Inject;
import java.util.Set;
import org.dataportabilityproject.gateway.action.Action;
import org.dataportabilityproject.gateway.action.ActionUtils;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Action} that handles listing the services available for export and import for a given
 * data type.
 */
public final class ListServicesAction implements
    Action<ListServicesActionRequest, ListServicesActionResponse> {

  private static final Logger logger = LoggerFactory.getLogger(
      ListServicesAction.class);

  private final AuthServiceProviderRegistry registry;

  @Inject
  ListServicesAction(
      AuthServiceProviderRegistry registry
  ) {
    this.registry = registry;
  }

  /**
   * Lists the services available for export and import for a given data type.
   */
  @Override
  public ListServicesActionResponse handle(ListServicesActionRequest request) {
    String transferDataType = request.getTransferDataType();
    // Validate incoming data type parameter
    if(!ActionUtils.isValidTransferDataType(transferDataType)){
      return ListServicesActionResponse.createWithError("Invalid transferDataType: " + transferDataType);
    }
    Set<String> services = registry.getServices(transferDataType);
    if (services.isEmpty()) {
      logger.warn("Empty service list found");
    }
    return ListServicesActionResponse.create(services);
  }
}
