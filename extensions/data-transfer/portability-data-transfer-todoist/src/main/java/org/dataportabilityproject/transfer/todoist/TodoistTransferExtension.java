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

package org.dataportabilityproject.transfer.todoist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.todoist.tasks.TodoistTasksExporter;
import org.dataportabilityproject.transfer.todoist.tasks.TodoistTasksImporter;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoistTransferExtension implements TransferExtension {
  private final Logger logger = LoggerFactory.getLogger(TodoistTransferExtension.class);
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("tasks");

  private Exporter<TokensAndUrlAuthData, TaskContainerResource> exporter;
  private Importer<TokensAndUrlAuthData, TaskContainerResource> importer;

  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "todoist";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "TodoistTransferExtension not initialized. Unable to get Exporter");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "TodoistTransferExtension not initialized. Unable to get Importer");
    Preconditions.checkArgument(false, "Instagram does not support import");
    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      logger.warn("TodoistTransferExtension already initialized");
      return;
    }

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    HttpTransport httpTransport = context.getService(HttpTransport.class);
    exporter = new TodoistTasksExporter(mapper, httpTransport);
    importer = new TodoistTasksImporter(mapper, httpTransport);
    initialized = true;
  }
}
