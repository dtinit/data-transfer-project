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

package org.datatransferproject.transfer.smugmug;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.smugmug.photos.SmugMugPhotosExporter;
import org.datatransferproject.transfer.smugmug.photos.SmugMugPhotosImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugTransferExtension implements TransferExtension {
  private static final List<String> SUPPORTED_TYPES = ImmutableList.of("photos");
  private static final Logger LOGGER = LoggerFactory.getLogger(SmugMugTransferExtension.class);
  private static final String SMUGMUG_KEY = "SMUGMUG_KEY";
  private static final String SMUGMUG_SECRET = "SMUGMUG_SECRET";

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
        SUPPORTED_TYPES.contains(transferDataType),
        "Export of " + transferDataType + " not supported by SmugMug");
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getImporter before initalizing SmugMugTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Import of " + transferDataType + " not supported by SmugMug");
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      LOGGER.warn("SmugMugTransferExtension already initailized.");
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
      LOGGER.warn(
          "Unable to retrieve client secrets. Did you set {} and {}?", SMUGMUG_KEY, SMUGMUG_SECRET);
      return;
    }

    ObjectMapper mapper = context.getService(TypeManager.class).getMapper();

    exporter = new SmugMugPhotosExporter(transport, appCredentials, mapper);
    importer = new SmugMugPhotosImporter(jobStore, transport, appCredentials, mapper);
    initialized = true;
  }
}
