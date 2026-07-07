/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon;

import com.google.common.base.Preconditions;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.amazon.photos.AmazonPhotosImporter;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

public class AmazonTransferExtension implements TransferExtension {

  private static final String SERVICE_ID = "Amazon";

  private AmazonPhotosImporter importer;
  private volatile boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    // TODO: Implement exporter
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(initialized, "Extension not initialized");
    Preconditions.checkArgument(transferDataType == DataVertical.PHOTOS);
    return importer;
  }

  @Override
  public synchronized void initialize(ExtensionContext context) {
    if (initialized) return;

    Monitor monitor = context.getMonitor();

    AppCredentials appCredentials;
    try {
      appCredentials = context.getService(AppCredentialStore.class)
          .getAppCredentials("AMAZON_KEY", "AMAZON_SECRET");
    } catch (IOException e) {
      monitor.severe(() -> "Unable to retrieve Amazon AppCredentials.", e);
      return;
    }

    importer = new AmazonPhotosImporter(
        monitor, appCredentials.getKey(), appCredentials.getSecret(),
        context.getService(TemporaryPerJobDataStore.class));

    initialized = true;
  }
}
