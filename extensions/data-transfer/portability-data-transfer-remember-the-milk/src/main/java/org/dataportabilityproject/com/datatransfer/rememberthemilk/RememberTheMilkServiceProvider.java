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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
 * The {@link ServiceProvider} for the Remember the Milk service (https://www.rememberthemilk.com/).
 */
final class RememberTheMilkServiceProvider implements ServiceProvider {

  private final static ImmutableList<PortableDataType> SUPPORTED_DATA_TYPES = ImmutableList
      .of(PortableDataType.TASKS);
  private final static Map<ServiceMode, RememberTheMilkAuth> AUTH_MAP = new HashMap<>();
  private final AppCredentials appCredentials;

  @Inject
  RememberTheMilkServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
    this.appCredentials = appCredentialFactory.lookupAndCreate("RTM_KEY", "RTM_SECRET");
  }

  @Override
  public String getName() {
    return "Remember the Milk";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    return lookupAndCreateAuth(dataType, serviceMode);
  }

  @Override
  public Exporter<? extends DataModel> getExporter(PortableDataType type,
      AuthData authData, JobDataCache jobDataCache) throws IOException {
    return getInstanceOfService(authData, jobDataCache,
        lookupAndCreateAuth(type, ServiceMode.EXPORT));
  }

  @Override
  public Importer<? extends DataModel> getImporter(PortableDataType type,
      AuthData authData, JobDataCache jobDataCache) throws IOException {
    return getInstanceOfService(authData, jobDataCache,
        lookupAndCreateAuth(type, ServiceMode.IMPORT));
  }

  private synchronized RememberTheMilkTaskService getInstanceOfService(
      AuthData authData, JobDataCache jobDataCache, RememberTheMilkAuth rememberTheMilkAuth)
      throws IOException {
    RememberTheMilkSignatureGenerator signer = new RememberTheMilkSignatureGenerator(
        appCredentials,
        rememberTheMilkAuth.getToken(authData));

    return new RememberTheMilkTaskService(signer, jobDataCache);
  }

  private RememberTheMilkAuth lookupAndCreateAuth(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(dataType),
        "[%s] mode not supported for dataType [%s]", serviceMode, dataType);
    if (!AUTH_MAP.containsKey(serviceMode)) {
      AUTH_MAP.put(serviceMode, new RememberTheMilkAuth(
          new RememberTheMilkSignatureGenerator(appCredentials, null), serviceMode));
    }
    return AUTH_MAP.get(serviceMode);
  }
}
