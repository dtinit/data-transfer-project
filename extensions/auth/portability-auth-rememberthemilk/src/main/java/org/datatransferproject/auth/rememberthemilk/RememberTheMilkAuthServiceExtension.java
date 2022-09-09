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

package org.datatransferproject.auth.rememberthemilk;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.datatransferproject.types.common.models.DataVertical.TASKS;

public class RememberTheMilkAuthServiceExtension implements AuthServiceExtension {
  private static final String RTM_KEY = "RTM_KEY";
  private static final String RTM_SECRET = "RTM_SECRET";
  private static final String SERVICE_ID = "REMEMBER_THE_MILK";

  private final List<DataVertical> supportedServices = ImmutableList.of(TASKS);
  private RememberTheMilkAuthDataGenerator importAuthDataGenerator;
  private RememberTheMilkAuthDataGenerator exportAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(DataVertical transferDataType, AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "RememberTheMilkAuthServiceExtension is not initialized! Unable to retrieve AuthDataGenerator");
    Preconditions.checkArgument(
        supportedServices.contains(transferDataType),
        "Transfer type [" + transferDataType + "] is not supported in RememberTheMilk");
    return (mode == AuthMode.IMPORT) ? importAuthDataGenerator : exportAuthDataGenerator;
  }

  @Override
  public List<DataVertical> getImportTypes() {
    return supportedServices;
  }

  @Override
  public List<DataVertical> getExportTypes() {
    return supportedServices;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    AppCredentials appCredentials;
    try {
      appCredentials =
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
    importAuthDataGenerator =
        new RememberTheMilkAuthDataGenerator(appCredentials, AuthMode.IMPORT, monitor);
    exportAuthDataGenerator =
        new RememberTheMilkAuthDataGenerator(appCredentials, AuthMode.EXPORT, monitor);
    initialized = true;
  }
}
