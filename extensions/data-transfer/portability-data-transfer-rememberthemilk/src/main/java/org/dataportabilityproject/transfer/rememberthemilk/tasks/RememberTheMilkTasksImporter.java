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

package org.dataportabilityproject.transfer.rememberthemilk.tasks;

import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.rememberthemilk.RememberTheMilkSignatureGenerator;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;

import java.io.IOException;
import java.util.UUID;

public class RememberTheMilkTasksImporter implements Importer<AuthData, TaskContainerResource> {
  private final JobStore jobstore;
  private final RememberTheMilkService rememberTheMilkService;

  RememberTheMilkTasksImporter(
      JobStore jobStore, RememberTheMilkSignatureGenerator signatureGenerator) {
    this.jobstore = jobStore;
    this.rememberTheMilkService = new RememberTheMilkService(signatureGenerator);
  }

  @Override
  public ImportResult importItem(UUID jobId, AuthData authData, TaskContainerResource data) {
    String timeline;
    try {
      timeline = rememberTheMilkService.createTimeline();

      for (TaskListModel taskList : data.getLists()) {
        ListInfo listInfo = rememberTheMilkService.createTaskList(taskList.getName(), timeline);
      }
    } catch (IOException e) {
      return new ImportResult(ImportResult.ResultType.ERROR);
    }
    return new ImportResult(ImportResult.ResultType.OK);
  }
}
