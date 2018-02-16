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

package org.dataportabilityproject.gateway;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.types.transfer.PortableType;

public class PortabilityAuthServiceProviderRegistry implements AuthServiceProviderRegistry {

  private final ImmutableMap<String, AuthServiceProvider> authServiceProviderMap;
  private final ImmutableSet<String> supportedImportTypes;
  private final ImmutableSet<String> supportedExportTypes;

  // The parameters to the constructor are provided via dependency injection
  public PortabilityAuthServiceProviderRegistry(List<String> enabledServices,
      Map<String, AuthServiceProvider> serviceProviderMap){
    ImmutableMap.Builder<String, AuthServiceProvider> serviceProviderBuilder = ImmutableMap
        .builder();
    ImmutableSet.Builder<String> supportedImportTypesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<String> supportedExportTypesBuilder = ImmutableSet.builder();

    for(String service : enabledServices){
      AuthServiceProvider authServiceProvider = serviceProviderMap.get(service);
      Preconditions.checkArgument(authServiceProvider != null, "AuthServiceProvider not found for [%s]", service);

      List<String> importTypes = authServiceProvider.getImportTypes();
      List<String> exportTypes = authServiceProvider.getExportTypes();

      for (String type : importTypes) {
        Preconditions.checkArgument(exportTypes.contains(type),
            "TransferDataType [%s] is available for import but not export in [%s] AuthServiceProvider",
            type, service);
        supportedImportTypesBuilder.add(type);
      }

      supportedExportTypesBuilder.addAll(exportTypes);
      serviceProviderBuilder.put(service, authServiceProvider);
    }

    authServiceProviderMap = serviceProviderBuilder.build();
    supportedImportTypes = supportedImportTypesBuilder.build();
    supportedExportTypes = supportedExportTypesBuilder.build();
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String serviceId, String transferDataType, AuthMode mode) {
    AuthServiceProvider provider = authServiceProviderMap.get(serviceId);
    Preconditions.checkArgument(provider!=null, "AuthServiceProvider not found for serviceId [%s]", serviceId);
    switch(mode) {
      case EXPORT:
        Preconditions.checkArgument(supportedExportTypes.contains(transferDataType), "AuthMode [%s] not valid for TransferDataType [%s]", mode, transferDataType);
        break;
      case IMPORT:
        Preconditions.checkArgument(supportedImportTypes.contains(transferDataType), "AuthMode [%s] not valid for TransferDataType [%s]", mode, transferDataType);
        break;
      default:
        throw new IllegalArgumentException("AuthMode [" + mode + "] not supported");
    }

    return provider.getAuthDataGenerator(transferDataType, mode);
  }
}
