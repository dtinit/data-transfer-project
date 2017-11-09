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

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.SecretAuthData;

public final class InstagramServiceProvider implements ServiceProvider {
  private static final ImmutableList<String> SCOPES = ImmutableList.of(
      "basic"); // See https://www.instagram.com/developer/authorization/
  private final InstagramAuth instagramAuth;

  public InstagramServiceProvider(Secrets secrets) {
    this.instagramAuth = new InstagramAuth(
        secrets.get("INSTAGRAM_CLIENT_ID"),
        secrets.get("INSTAGRAM_SECRET"),
        SCOPES);
  }

  @Override
  public String getName() {
    return "Instagram";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return ImmutableList.of(PortableDataType.PHOTOS);
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    // Currently Instagram doesn't support import.
    return ImmutableList.of();
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
    return instagramAuth;
  }

  @Override
  public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType) {
    return instagramAuth;
  }

  @Override
  public Exporter<? extends DataModel> getExporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    if (type == PortableDataType.PHOTOS) {
      return new InstagramPhotoService(((SecretAuthData) authData).secret());
    }
    throw new IllegalStateException("Instagram doesn't support exporting: " + type);
  }

  @Override
  public Importer<? extends DataModel> getImporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    throw new IllegalStateException("Instagram doesn't support importing anything");
  }
}
