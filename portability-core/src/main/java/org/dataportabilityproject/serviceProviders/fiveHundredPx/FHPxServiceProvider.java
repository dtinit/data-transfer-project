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
package org.dataportabilityproject.serviceProviders.fiveHundredPx;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for the 500px service (https://www.500px.com/).
 */
final public class FHPxServiceProvider implements ServiceProvider {

  private final static ImmutableList<PortableDataType> SUPPORTED_DATA_TYPES = ImmutableList
      .of(PortableDataType.PHOTOS);
  private final static Map<ServiceMode, FHPxAuth> AUTH_MAP = new HashMap<>();
  private final AppCredentials appCredentials;

  @Inject
  FHPxServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
    this.appCredentials =
        appCredentialFactory.lookupAndCreate("500PX_KEY", "500PX_SECRET");
  }

  @Override
  public String getName() {
    return "500px";
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
  public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions
        .checkArgument(SUPPORTED_DATA_TYPES.contains(dataType),
            "Export of type [%s] is not supported by Instagram",
            dataType);
    return lookupAndCreateAuth(dataType, serviceMode);
  }

  @Override
  public Exporter<? extends DataModel> getExporter(PortableDataType type, AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    return getInstanceOfService(authData, jobDataCache,
        lookupAndCreateAuth(type, ServiceMode.EXPORT));
  }

  @Override
  public Importer<? extends DataModel> getImporter(PortableDataType type, AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    return getInstanceOfService(authData, jobDataCache,
        lookupAndCreateAuth(type, ServiceMode.IMPORT));
  }

  private synchronized FHPxPhotoService getInstanceOfService(AuthData authData,
      JobDataCache jobDataCache, FHPxAuth fhPxAuth) throws IOException {
    OAuthConsumer consumer = fhPxAuth.generateConsumer(authData);
    return new FHPxPhotoService(consumer, jobDataCache);
  }

  private FHPxAuth lookupAndCreateAuth(PortableDataType dataType, ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(dataType),
        "[%s] mode not supported for dataType [%s]", serviceMode, dataType);
    if (!AUTH_MAP.containsKey(serviceMode)) {
      AUTH_MAP.put(serviceMode, new FHPxAuth(appCredentials, serviceMode));
    }
    return AUTH_MAP.get(serviceMode);
  }
}
