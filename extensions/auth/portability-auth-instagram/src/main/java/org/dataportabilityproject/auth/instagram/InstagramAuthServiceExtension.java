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
package org.dataportabilityproject.auth.instagram;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstagramAuthServiceExtension implements AuthServiceExtension {
  private final Logger logger = LoggerFactory.getLogger(InstagramAuthServiceExtension.class);
  private static final String INSTAGRAM_KEY = "INSTAGRAM_KEY";
  private static final String INSTAGRAM_SECRET = "INSTAGRAM_SECRET";
  private static final String INSTAGRAM_SERVICE_ID = "remember the milk";
  private final List<String> SUPPORTED_SERVICES = ImmutableList.of("photos");
  private InstagramAuthDataGenerator importAuthDataGenerator;
  private InstagramAuthDataGenerator exportAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return INSTAGRAM_SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "InstagramAuthServiceExtension is not initialized! Unable to retrieve AuthDataGenerator");
    Preconditions.checkArgument(
        SUPPORTED_SERVICES.contains(transferDataType),
        "Transfer type [" + transferDataType + "] is not supported in Instagram");
    return (mode == AuthMode.IMPORT) ? importAuthDataGenerator : exportAuthDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return SUPPORTED_SERVICES;
  }

  @Override
  public List<String> getExportTypes() {
    return SUPPORTED_SERVICES;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized)  {
      logger.warn("InstagramAuthServiceExtension already initalized");
      return;
    }

    AppCredentials appCredentials;
    try {
      appCredentials =
          context.getService(AppCredentialStore.class).getAppCredentials(INSTAGRAM_KEY, INSTAGRAM_SECRET);
    } catch (IOException e) {
      logger.warn(
          "Error retrieving Instagram Credentials. Did you set {} and {}?",
          INSTAGRAM_KEY,
          INSTAGRAM_SECRET);
      return;
    }

    importAuthDataGenerator =
        new InstagramAuthDataGenerator();
    exportAuthDataGenerator =
        new InstagramAuthDataGenerator();
    initialized = true;
  }
}
