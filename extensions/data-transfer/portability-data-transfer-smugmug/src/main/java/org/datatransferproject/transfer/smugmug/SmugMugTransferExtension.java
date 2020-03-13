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
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.smugmug.photos.SmugMugPhotosExporter;
import org.datatransferproject.transfer.smugmug.photos.SmugMugPhotosImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

import static java.lang.String.format;

public class SmugMugTransferExtension implements TransferExtension {

  private static final ImmutableList<String> SUPPORTED_TYPES = ImmutableList.of("PHOTOS");
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
    Monitor monitor = context.getMonitor();
    if (initialized) {
      monitor.severe(() -> "SmugMugTransferExtension already initailized.");
      return;
    }

    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(SMUGMUG_KEY, SMUGMUG_SECRET);
    } catch (IOException e) {
      monitor.info(
          () ->
              format(
                  "Unable to retrieve SmugMug AppCredentials. Did you set %s and %s?",
                  SMUGMUG_KEY, SMUGMUG_SECRET),
          e);
      return;
    }

    ObjectMapper mapper = context.getService(TypeManager.class).getMapper();

    exporter = new SmugMugPhotosExporter(appCredentials, mapper, jobStore, monitor);
    importer = new SmugMugPhotosImporter(jobStore, appCredentials, mapper, monitor);
    initialized = true;
  }
}
