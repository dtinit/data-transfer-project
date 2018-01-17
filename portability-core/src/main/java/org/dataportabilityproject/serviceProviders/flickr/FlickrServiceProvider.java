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
package org.dataportabilityproject.serviceProviders.flickr;

import com.flickr4java.flickr.auth.Auth;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.IOException;
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
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for the Flickr service (http://www.flickr.com/).
 */
final class FlickrServiceProvider implements ServiceProvider {
    private final AppCredentials appCredentials;
    private final FlickrAuth importAuth;
    private final FlickrAuth exportAuth;
    private final ImmutableList<PortableDataType> dataTypes = ImmutableList
        .of(PortableDataType.PHOTOS);

    @Inject
    FlickrServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
        this.appCredentials = appCredentialFactory.lookupAndCreate("FLICKR_KEY", "FLICKR_SECRET");
        this.importAuth = new FlickrAuth(appCredentials, ServiceMode.IMPORT);
        this.exportAuth = new FlickrAuth(appCredentials, ServiceMode.EXPORT);
    }

    @Override public String getName() {
        return "Flickr";
    }

    @Override public ImmutableList<PortableDataType> getExportTypes() {
        return dataTypes;
    }

    @Override public ImmutableList<PortableDataType> getImportTypes() {
        return dataTypes;
    }

    @Override // OfflineDataGenerator
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType, ServiceMode serviceMode) {
        Preconditions.checkArgument(dataTypes.contains(dataType),
                "Provided dataType not supported: " + dataType);

        switch (serviceMode) {
            case IMPORT:
                return importAuth;
            case EXPORT:
                return exportAuth;
            default:
                throw new IllegalArgumentException("ServiceMode not supported: " + serviceMode);
        }
    }

    @Override
    public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType,
        ServiceMode serviceMode) {
        Preconditions.checkArgument(dataTypes.contains(dataType),
            "Provided dataType not supported: " + dataType);

        switch (serviceMode) {
            case IMPORT:
                return importAuth;
            case EXPORT:
                return exportAuth;
            default:
                throw new IllegalArgumentException("ServiceMode not supported: " + serviceMode);
        }
    }

    @Override public Exporter<? extends DataModel> getExporter(
        PortableDataType type,
        AuthData authData,
        JobDataCache jobDataCache) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData, jobDataCache, ServiceMode.EXPORT);
    }

    @Override public Importer<? extends DataModel> getImporter(
        PortableDataType type,
        AuthData authData,
        JobDataCache jobDataCache) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData, jobDataCache, ServiceMode.IMPORT);
    }

    private synchronized FlickrPhotoService getInstanceOfService(
        AuthData authData,
        JobDataCache jobDataCache, ServiceMode serviceMode) throws IOException {
        Auth auth;

        switch (serviceMode) {
            case EXPORT:
                auth = exportAuth.getAuth(authData);
                break;
            case IMPORT:
                auth = importAuth.getAuth(authData);
                break;
            default:
                throw new IllegalArgumentException("ServiceMode not supported: " + serviceMode);
        }

        return new FlickrPhotoService(
                appCredentials,
                auth,
                jobDataCache);
    }
}
