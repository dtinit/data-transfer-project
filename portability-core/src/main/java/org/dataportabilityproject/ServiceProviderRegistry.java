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
package org.dataportabilityproject;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry of all the supported {@link org.dataportabilityproject.shared.ServiceProvider}
 */
public class ServiceProviderRegistry {
    private final Logger logger = LoggerFactory.getLogger(ServiceProviderRegistry.class);
    private final ImmutableMap<String, ServiceProvider> serviceProviders;
    private final CloudFactory cloudFactory;
    private final ImmutableSet<PortableDataType> supportedTypes;

    @Inject
    public ServiceProviderRegistry(
        CloudFactory cloudFactory,
        CommonSettings commonSettings,
        Map<String, ServiceProvider> serviceProviderMap)
            throws Exception {
        this.cloudFactory = cloudFactory;
        ImmutableMap.Builder<String, ServiceProvider> providerBuilder = ImmutableMap.builder();
        ImmutableSet.Builder<PortableDataType> portableDataTypesBuilder = ImmutableSet.builder();

        for(String enabledService : commonSettings.getServiceProviderClasses()) {
            ServiceProvider serviceProvider = serviceProviderMap.get(enabledService);
            checkState(serviceProvider != null, "Couldn't find %s", enabledService);
            providerBuilder.put(serviceProvider.getName(), serviceProvider);
            portableDataTypesBuilder.addAll(serviceProvider.getImportTypes());
            portableDataTypesBuilder.addAll(serviceProvider.getExportTypes());
        }

        this.serviceProviders = providerBuilder.build();
        this.supportedTypes = portableDataTypesBuilder.build();

        if (this.serviceProviders.isEmpty()) {
            throw new IllegalStateException("No service providers were provided");
        }
    }

    public ImmutableSet<PortableDataType> getSupportedTypes() {
        return supportedTypes;
    }

    public List<String> getServiceProvidersThatCanExport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getExportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(ServiceProvider::getName)
                .collect(Collectors.toList());
    }

    public List<String> getServiceProvidersThatCanImport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getImportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(ServiceProvider::getName)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends DataModel> Exporter<T> getExporter(
            String serviceProvider,
            PortableDataType portableDataType,
            String jobId,
            AuthData authData) throws IOException {
        JobDataCache jobDataCache = cloudFactory.getJobDataCache(jobId, serviceProvider);
        Exporter<? extends DataModel> exporter = serviceProviders.get(serviceProvider)
            .getExporter(portableDataType, authData, jobDataCache);
        return (Exporter<T>) exporter;
    }

    @SuppressWarnings("unchecked")
    public <T extends DataModel> Importer<T> getImporter(
            String serviceProvider,
            PortableDataType portableDataType,
            String jobId,
            AuthData authData) throws IOException {
        JobDataCache jobDataCache = cloudFactory.getJobDataCache(jobId, serviceProvider);
        Importer<? extends DataModel> importer = serviceProviders.get(serviceProvider)
            .getImporter(portableDataType, authData, jobDataCache);
        return (Importer<T>) importer;
    }

    public OnlineAuthDataGenerator getOnlineAuth(String serviceProvider,
                                                 PortableDataType dataType, ServiceMode serviceMode) {
        return serviceProviders.get(serviceProvider).getOnlineAuthDataGenerator(dataType, serviceMode);
    }

    public OfflineAuthDataGenerator getOfflineAuth(String serviceProvider,
            PortableDataType dataType, ServiceMode serviceMode) {
        return serviceProviders.get(serviceProvider).getOfflineAuthDataGenerator(dataType, serviceMode);
    }
}
