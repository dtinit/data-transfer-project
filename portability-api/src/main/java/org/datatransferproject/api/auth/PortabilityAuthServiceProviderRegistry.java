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

package org.datatransferproject.api.auth;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PortabilityAuthServiceProviderRegistry implements AuthServiceProviderRegistry {
  private final ImmutableMap<String, AuthServiceExtension> authServiceProviderMap;
  private final ImmutableSet<String> supportedImportTypes;
  private final ImmutableSet<String> supportedExportTypes;

  @Inject
  public PortabilityAuthServiceProviderRegistry(
      Map<String, AuthServiceExtension> serviceProviderMap) {

    ImmutableMap.Builder<String, AuthServiceExtension> serviceProviderBuilder =
        ImmutableMap.builder();
    ImmutableSet.Builder<String> supportedImportTypesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<String> supportedExportTypesBuilder = ImmutableSet.builder();

    serviceProviderMap.forEach(
        (service, provider) -> {
          List<String> importTypes = provider.getImportTypes();
          List<String> exportTypes = provider.getExportTypes();
          for (String type : importTypes) {
            Preconditions.checkArgument(
                exportTypes.contains(type),
                "TransferDataType [%s] is available for import but not export in [%s] AuthServiceExtension",
                type,
                service);
            supportedImportTypesBuilder.add(type);
          }
          supportedExportTypesBuilder.addAll(exportTypes);
          serviceProviderBuilder.put(service, provider);
        });

    authServiceProviderMap = serviceProviderBuilder.build();
    supportedImportTypes = supportedImportTypesBuilder.build();
    supportedExportTypes = supportedExportTypesBuilder.build();
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(
      String serviceId, String transferDataType, AuthMode mode) {
    AuthServiceExtension provider = authServiceProviderMap.get(serviceId);
    Preconditions.checkArgument(
        provider != null, "AuthServiceExtension not found for serviceId [%s]", serviceId);
    switch (mode) {
      case EXPORT:
        Preconditions.checkArgument(
            supportedExportTypes.contains(transferDataType),
            "AuthMode [%s] not valid for TransferDataType [%s]",
            mode,
            transferDataType);
        break;
      case IMPORT:
        Preconditions.checkArgument(
            supportedImportTypes.contains(transferDataType),
            "AuthMode [%s] not valid for TransferDataType [%s]",
            mode,
            transferDataType);
        break;
      default:
        throw new IllegalArgumentException("AuthMode [" + mode + "] not supported");
    }

    return provider.getAuthDataGenerator(transferDataType, mode);
  }

  @Override
  public Set<String> getImportServices(String transferDataType) {
    Preconditions.checkArgument(
        supportedImportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for import",
        transferDataType);
    return authServiceProviderMap
        .values()
        .stream()
        .filter(
            sp -> sp.getImportTypes().stream().anyMatch(e -> e.equalsIgnoreCase(transferDataType)))
        .map(AuthServiceExtension::getServiceId)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getExportServices(String transferDataType) {
    Preconditions.checkArgument(
        supportedExportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for export",
        transferDataType);
    return authServiceProviderMap
        .values()
        .stream()
        .filter(
            sp -> sp.getExportTypes().stream().anyMatch(e -> e.equalsIgnoreCase(transferDataType)))
        .map(AuthServiceExtension::getServiceId)
        .collect(Collectors.toSet());
  }

  /** Returns the set of data types that support both import and export. */
  @Override
  public Set<String> getTransferDataTypes() {
    return Sets.intersection(supportedExportTypes, supportedImportTypes);
  }
}
