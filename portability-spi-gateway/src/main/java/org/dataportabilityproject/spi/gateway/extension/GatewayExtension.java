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
package org.dataportabilityproject.spi.gateway.extension;

import org.dataportabilityproject.api.launcher.AbstractExtension;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
import org.dataportabilityproject.spi.gateway.provider.ServiceProviderInfo;

import java.util.List;

/**
 * Gateway extensions implement this interface to be loaded in a gateway process.
 */
public interface GatewayExtension extends AbstractExtension {

    /**
     * Returns service provider information supported by this extension.
     */
    List<ServiceProviderInfo> getServiceProviderInfos();

    /**
     * Returns initialized extension exporters.
     */
    List<AuthServiceProvider> getAuthServiceProviders();
}
