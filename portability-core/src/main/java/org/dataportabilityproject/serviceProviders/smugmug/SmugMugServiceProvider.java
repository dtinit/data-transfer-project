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
package org.dataportabilityproject.serviceProviders.smugmug;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.IOException;
import oauth.signpost.OAuthConsumer;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.AppCredentialFactory;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for the SmugMub service (https://www.smugmug.com/).
 */
final class SmugMugServiceProvider implements ServiceProvider {

    private final SmugMugAuth importAuth;
    private final SmugMugAuth exportAuth;

    private final static ImmutableList<PortableDataType> SUPPORTED_DATA_TYPES = ImmutableList
        .of(PortableDataType.PHOTOS);

    @Inject
    SmugMugServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
        AppCredentials appCredentials =
            appCredentialFactory.lookupAndCreate("SMUGMUG_KEY", "SMUGMUG_SECRET");
        this.importAuth = new SmugMugAuth(appCredentials, ServiceMode.IMPORT);
        this.exportAuth = new SmugMugAuth(appCredentials, ServiceMode.EXPORT);
    }

    @Override public String getName() {
        return "SmugMug";
    }

    @Override public ImmutableList<PortableDataType> getExportTypes() {
        return SUPPORTED_DATA_TYPES;
    }

    @Override public ImmutableList<PortableDataType> getImportTypes() {
        return SUPPORTED_DATA_TYPES;
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType,
        ServiceMode serviceMode) {
        Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(dataType),
            "[%s] mode is not supported for dataType [%s] by Smugmug.", serviceMode, dataType);
        switch (serviceMode) {
            case EXPORT:
                return exportAuth;
            case IMPORT:
                return importAuth;
            default:
                throw new IllegalArgumentException("Unsupported service mode: " + serviceMode);
        }
    }

    @Override public Exporter<? extends DataModel> getExporter(PortableDataType type,
            AuthData authData, JobDataCache jobDataCache) throws IOException {
        Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(type),
            "Export of type [%s] is not supported by SmugMug.", type);
        return getInstanceOfService(authData, jobDataCache, exportAuth);
    }

    @Override public Importer<? extends DataModel> getImporter(PortableDataType type,
            AuthData authData, JobDataCache jobDataCache) throws IOException {
        Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(type),
            "Import of type [%s] is not supported by SmugMug.", type);

        return getInstanceOfService(authData, jobDataCache, importAuth);
    }

    private synchronized SmugMugPhotoService getInstanceOfService(
        AuthData authData,
        JobDataCache jobDataCache, SmugMugAuth smugMugAuth) throws IOException {
        OAuthConsumer consumer = smugMugAuth.generateConsumer(authData);
        return new SmugMugPhotoService(
            consumer,
            jobDataCache);
    }
}
