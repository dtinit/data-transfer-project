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
package org.dataportabilityproject.serviceProviders.instagram;

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

final class InstagramServiceProvider implements ServiceProvider {

  // Instagram only offers basic scope for reading user's profiles. There is no "write" scope.
  // See https://www.instagram.com/developer/authorization/
  private static final ImmutableList<String> SCOPES = ImmutableList.of("basic");
  private static final ImmutableList<PortableDataType> EXPORT_TYPES = ImmutableList
      .of(PortableDataType.PHOTOS);

  private final InstagramAuth instagramAuth;

  @Inject
  InstagramServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
    AppCredentials appCredentials =
        appCredentialFactory.lookupAndCreate("INSTAGRAM_KEY", "INSTAGRAM_SECRET");
    this.instagramAuth = new InstagramAuth(appCredentials, SCOPES);
  }

  @Override
  public String getName() {
    return "Instagram";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return EXPORT_TYPES;
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    // Currently Instagram doesn't support import.
    return ImmutableList.of();
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions
        .checkArgument(serviceMode == ServiceMode.EXPORT, "IMPORT not supported by Instagram");
    Preconditions
        .checkArgument(EXPORT_TYPES.contains(dataType),
            "Export of type [%s] is not supported by Instagram",
            dataType);
    return instagramAuth;
  }

  @Override
  public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions
        .checkArgument(serviceMode == ServiceMode.EXPORT, "IMPORT not supported by Instagram");
    Preconditions
        .checkArgument(EXPORT_TYPES.contains(dataType),
            "Export of type [%s] is not supported by Instagram",
            dataType);
    return instagramAuth;
  }

  @Override
  public Exporter<? extends DataModel> getExporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    Preconditions
        .checkArgument(EXPORT_TYPES.contains(type),
            "Export of type [%s] is not supported by Instagram", type);
    return new InstagramPhotoService(((InstagramOauthData) authData));
  }

  @Override
  public Importer<? extends DataModel> getImporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    throw new IllegalStateException("Instagram doesn't support importing anything");
  }
}
