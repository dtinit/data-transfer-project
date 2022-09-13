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

package org.datatransferproject.transfer.rememberthemilk;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.rememberthemilk.tasks.RememberTheMilkTasksExporter;
import org.datatransferproject.transfer.rememberthemilk.tasks.RememberTheMilkTasksImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

import static java.lang.String.format;
import static org.datatransferproject.types.common.models.DataVertical.TASKS;

public class RememberTheMilkTransferExtension implements TransferExtension {
  private static final ImmutableList<DataVertical> SUPPORTED_DATA_TYPES = ImmutableList.of(TASKS);
  private static final String RTM_KEY = "RTM_KEY";
  private static final String RTM_SECRET = "RTM_SECRET";
  private RememberTheMilkTasksExporter exporter;
  private RememberTheMilkTasksImporter importer;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "REMEMBER_THE_MILK";
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "RememberTheMilkTransferExtension not initialized. Unable to get Exporter");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "RememberTheMilkTransferExtension not initialized. Unable to get Importer");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    AppCredentials credentials;
    try {
      credentials =
          context.getService(AppCredentialStore.class).getAppCredentials(RTM_KEY, RTM_SECRET);
    } catch (IOException e) {
      Monitor monitor = context.getMonitor();
      monitor.info(
          () ->
              format(
                  "Unable to retrieve RememberTheMilk AppCredentials. Did you set %s and %s?",
                  RTM_KEY, RTM_SECRET),
          e);
      return;
    }

    Monitor monitor = context.getMonitor();

    exporter = new RememberTheMilkTasksExporter(credentials);
    importer = new RememberTheMilkTasksImporter(credentials, monitor);

    initialized = true;
  }
}
