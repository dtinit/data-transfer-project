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
import java.io.IOException;
import java.util.List;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberTheMilkAuthServiceExtension implements AuthServiceExtension {
  private static final String RTM_KEY = "RTM_KEY";
  private static final String RTM_SECRET = "RTM_SECRET";
  private static final String SERVICE_ID = "REMEMBER_THE_MILK";

  private final Logger logger = LoggerFactory.getLogger(RememberTheMilkAuthServiceExtension.class);
  private final List<String> supportedServices = ImmutableList.of("TASKS");
  private RememberTheMilkAuthDataGenerator importAuthDataGenerator;
  private RememberTheMilkAuthDataGenerator exportAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "RememberTheMilkAuthServiceExtension is not initialized! Unable to retrieve AuthDataGenerator");
    Preconditions.checkArgument(
        supportedServices.contains(transferDataType),
        "Transfer type [" + transferDataType + "] is not supported in RememberTheMilk");
    return (mode == AuthMode.IMPORT) ? importAuthDataGenerator : exportAuthDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return supportedServices;
  }

  @Override
  public List<String> getExportTypes() {
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
      logger.warn(
          "Error retrieving RememberTheMilk Credentials. Did you set {} and {}?",
          RTM_KEY,
          RTM_SECRET);
      return;
    }

    importAuthDataGenerator = new RememberTheMilkAuthDataGenerator(appCredentials, AuthMode.IMPORT);
    exportAuthDataGenerator = new RememberTheMilkAuthDataGenerator(appCredentials, AuthMode.EXPORT);
    initialized = true;
  }
}
