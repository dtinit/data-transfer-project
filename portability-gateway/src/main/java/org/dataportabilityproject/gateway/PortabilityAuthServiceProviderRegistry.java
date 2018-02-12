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

  private ImmutableMap<String, AuthServiceProvider> authServiceProviders;
  private ImmutableSet<PortableType> supportedTypes;

  PortabilityAuthServiceProviderRegistry(Map<String, AuthServiceProvider> serviceProviderMap, List<String> enabledServices){
    ImmutableSet.Builder<PortableType> supportedTypeBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<String, AuthServiceProvider> serviceProviderBuilder = ImmutableMap.builder();

    for(String service : enabledServices){
      AuthServiceProvider authServiceProvider = serviceProviderMap.get(service);
      Preconditions.checkArgument(authServiceProvider != null, "AuthService %s not found", service);
      serviceProviderBuilder.put(service, authServiceProvider);
    }
  }
  /**
   * Returns the provider that supports the service id.
   *
   * @param serviceId the service id
   */
  @Override
  public AuthServiceProvider getServiceProvider(String serviceId) {
    return null;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String serviceId, String transferDataType) {
    return null;
  }
}
