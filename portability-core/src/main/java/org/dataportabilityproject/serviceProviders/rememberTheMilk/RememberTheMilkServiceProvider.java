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
package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for the Remember the Milk service (https://www.rememberthemilk.com/).
 */
public final class RememberTheMilkServiceProvider implements ServiceProvider {
    private final AppCredentials appCredentials;
    private final RememberTheMilkAuth rememberTheMilkAuth;

    public RememberTheMilkServiceProvider() throws IOException {
        this.appCredentials = AppCredentials.lookupAndCreate("RTM_KEY", "RTM_SECRET");
        this.rememberTheMilkAuth = new RememberTheMilkAuth(
            new RememberTheMilkSignatureGenerator(
            appCredentials,
            null
        ));
    }

    @Override public String getName() {
        return "Remember the Milk";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.TASKS);
    }

    @Override
    public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(PortableDataType.TASKS);
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
        return rememberTheMilkAuth;
    }

    @Override public Exporter<? extends DataModel> getExporter(PortableDataType type,
            AuthData authData, JobDataCache jobDataCache) throws IOException {
        if (type != PortableDataType.TASKS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData, jobDataCache);
    }

    @Override public Importer<? extends DataModel> getImporter(PortableDataType type,
            AuthData authData, JobDataCache jobDataCache) throws IOException {
        if (type != PortableDataType.TASKS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData, jobDataCache);
    }

    private synchronized RememberTheMilkTaskService getInstanceOfService(
        AuthData authData, JobDataCache jobDataCache)
            throws IOException {
            RememberTheMilkSignatureGenerator signer = new RememberTheMilkSignatureGenerator(
                appCredentials,
                rememberTheMilkAuth.getToken(authData));

        return new RememberTheMilkTaskService(signer, jobDataCache);
    }
}