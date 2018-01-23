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
package org.dataportabilityproject.spi.gateway.provider;

import java.util.List;

/**
 * Tracks service provider importer and exporter extensions registered in the system.
 */
public interface ProviderInfoRegistry {

    /**
     * Returns the registered service providers.
     */
    List<ServiceProviderInfo> getRegisteredServiceProviders();

    /**
     * Registers a service provider.
     *
     * @param provider the provider to register
     */
    void registerServiceProvider(ServiceProviderInfo provider);

}
