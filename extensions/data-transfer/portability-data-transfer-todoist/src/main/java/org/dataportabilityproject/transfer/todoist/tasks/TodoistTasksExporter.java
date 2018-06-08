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

package org.dataportabilityproject.transfer.todoist.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import java.util.Optional;
import java.util.UUID;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;

public class TodoistTasksExporter implements Exporter<TokensAndUrlAuthData, TaskContainerResource> {

  private final ObjectMapper objectMapper;
  private final HttpTransport httpTransport;

  public TodoistTasksExporter(ObjectMapper objectMapper, HttpTransport httpTransport) {
    this.objectMapper = objectMapper;
    this.httpTransport = httpTransport;
  }

  @Override
  public ExportResult<TaskContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) {
    return null;
  }
}
