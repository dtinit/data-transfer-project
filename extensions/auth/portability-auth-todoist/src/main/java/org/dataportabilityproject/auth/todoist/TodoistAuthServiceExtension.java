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

package org.dataportabilityproject.auth.todoist;

import com.google.api.client.http.HttpTransport;
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

public class TodoistAuthServiceExtension implements AuthServiceExtension {

  private static final String TODOIST_KEY = "TODOIST_KEY";
  private static final String TODOIST_SECRET = "TODOIST_SECRET";
  private static final String TODOIST_SERVICE_ID = "todoist";
  private final Logger logger = LoggerFactory.getLogger(TodoistAuthServiceExtension.class);
  private final List<String> SUPPORTED_SERVICES = ImmutableList.of("tasks");

  private TodoistAuthDataGenerator exportAuthDataGenerator;
  private TodoistAuthDataGenerator importAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return TODOIST_SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "TodoistAuthServiceExtension is not initialized! Unable to retrieve AuthDataGenerator");
    Preconditions.checkArgument(
        SUPPORTED_SERVICES.contains(transferDataType),
        "Transfer type [" + transferDataType + "] is not supported in Todoist");
    return mode == AuthMode.EXPORT ? exportAuthDataGenerator : importAuthDataGenerator;
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
    if (initialized) {
      logger.warn("TodoistAuthServiceExtension already initialized");
      return;
    }

    System.out.println("Getting to todoist auth service extension");

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(TODOIST_KEY, TODOIST_SECRET);
    } catch (IOException e) {
      logger.warn(
          "Error retrieving Todoist credentials. Did you set {} and {}?",
          TODOIST_KEY,
          TODOIST_SECRET);
      return;
    }

    exportAuthDataGenerator =
        new TodoistAuthDataGenerator(appCredentials, context.getService(HttpTransport.class),
            AuthMode.EXPORT);
    importAuthDataGenerator = new TodoistAuthDataGenerator(appCredentials,
        context.getService(HttpTransport.class), AuthMode.IMPORT);

    initialized = true;
  }
}
