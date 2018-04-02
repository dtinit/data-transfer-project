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

package org.dataportabilityproject.transfer.rememberthemilk;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.rememberthemilk.tasks.RememberTheMilkTasksExporter;
import org.dataportabilityproject.transfer.rememberthemilk.tasks.RememberTheMilkTasksImporter;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

public class RememberTheMilkTransferExtension implements TransferExtension {
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("tasks");
  private static final String RTM_KEY = "RTM_KEY";
  private static final String RTM_SECRET = "RTM_SECRET";

  private RememberTheMilkTasksExporter exporter;
  private RememberTheMilkTasksImporter importer;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "remember the milk";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    JobStore jobStore = context.getService(JobStore.class);
    AppCredentials credentials;
    try {
      credentials =
          context.getService(AppCredentialStore.class).getAppCredentials(RTM_KEY, RTM_SECRET);
    } catch (IOException e) {
      return;
    }

    exporter = new RememberTheMilkTasksExporter(credentials);
    importer = new RememberTheMilkTasksImporter(credentials, jobStore);

    initialized = true;
  }
}
