/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.facebook;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.facebook.photos.FacebookPhotosExporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FacebookTransferExtension implements TransferExtension {
  private static final Logger logger = LoggerFactory.getLogger(FacebookTransferExtension.class);
  private static final String SERVICE_ID = "Facebook";
  private boolean initialized = false;

  private static final ImmutableList<String> SUPPORTED_SERVICES = ImmutableList.of("photos");
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("FACEBOOK_KEY", "FACEBOOK_SECRET");
    } catch (IOException e) {
      logger.warn(
          "Problem getting AppCredentials: {}. Did you set FACEBOOK_KEY and FACEBOOK_SECRET?", e);
      return;
    }

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put("photos", new FacebookPhotosExporter(appCredentials));
    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
