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

package org.dataportabilityproject.transfer.smugmug;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.smugmug.photos.SmugMugPhotosExporter;
import org.dataportabilityproject.transfer.smugmug.photos.SmugMugPhotosImporter;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugTransferExtension implements TransferExtension {
  private final List<String> supportedTypes = ImmutableList.of("photos");
  private final Logger logger = LoggerFactory.getLogger(SmugMugTransferExtension.class);
  private final String SMUGMUG_KEY = "SMUGMUG_KEY";
  private final String SMUGMUG_SECRET = "SMUGMUG_SECRET";
  private SmugMugPhotosImporter importer;
  private SmugMugPhotosExporter exporter;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "SmugMug";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getExporter before initalizing SmugMugTransferExtension");
    Preconditions.checkArgument(
        supportedTypes.contains(transferDataType),
        "Export of " + transferDataType + " not supported by SmugMug");
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getImporter before initalizing SmugMugTransferExtension");
    Preconditions.checkArgument(
        supportedTypes.contains(transferDataType),
        "Import of " + transferDataType + " not supported by SmugMug");
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      logger.warn("SmugMugTransferExtension already initailized.");
      return;
    }

    HttpTransport transport = context.getService(HttpTransport.class);
    JobStore jobStore = context.getService(JobStore.class);

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(SMUGMUG_KEY, SMUGMUG_SECRET);
    } catch (IOException e) {
      logger.warn(
          "Unable to retrieve client secrets. Did you set {} and {}?", SMUGMUG_KEY, SMUGMUG_SECRET);
      return;
    }

    exporter = new SmugMugPhotosExporter(transport, appCredentials);
    importer = new SmugMugPhotosImporter(jobStore, transport, appCredentials);
    initialized = true;
  }
}
