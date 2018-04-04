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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.UUID;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempTasksData;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Importer for Tasks data type to Remember The Milk Service.
 */
public class RememberTheMilkTasksImporter implements Importer<AuthData, TaskContainerResource> {
  private final JobStore jobstore;
  private final AppCredentials appCredentials;
  private final Logger logger = LoggerFactory.getLogger(RememberTheMilkTasksImporter.class);
  private RememberTheMilkService service;

  public RememberTheMilkTasksImporter(AppCredentials appCredentials, JobStore jobStore) {
    this.jobstore = jobStore;
    this.appCredentials = appCredentials;
    this.service = null;
  }

  @VisibleForTesting
  public RememberTheMilkTasksImporter(
      AppCredentials appCredentials, JobStore jobStore, RememberTheMilkService service) {
    this.jobstore = jobStore;
    this.appCredentials = appCredentials;
    this.service = service;
  }

  @Override
  public ImportResult importItem(UUID jobId, AuthData authData, TaskContainerResource data) {
    String timeline;

    TempTasksData tempTasksData = jobstore.findData(TempTasksData.class, jobId);
    if (tempTasksData == null) {
      tempTasksData = new TempTasksData(jobId.toString());
      jobstore.create(jobId, tempTasksData);
    }

    try {
      RememberTheMilkService service = getOrCreateService(authData);

      timeline = service.createTimeline();

      for (TaskListModel taskList : data.getLists()) {
        ListInfo listInfo = service.createTaskList(taskList.getName(), timeline);
        tempTasksData.addTaskListId(taskList.getId(), Long.toString(listInfo.id));
      }
      jobstore.update(jobId, tempTasksData);

      for (TaskModel task : data.getTasks()) {
        String newList = tempTasksData.lookupNewTaskListId(task.getTaskListId());
        TaskSeries addedTask = service.createTask(task.getText(), timeline, newList);
        // todo: add notes
      }
    } catch (Exception e) {
      logger.warn("Error importing item: " + e.getMessage());
      logger.warn(Throwables.getStackTraceAsString(e));
      return new ImportResult(ImportResult.ResultType.ERROR, e.getMessage());
    }
    return new ImportResult(ImportResult.ResultType.OK);
  }

  private RememberTheMilkService getOrCreateService(AuthData authData) {
    Preconditions.checkArgument(authData instanceof TokenAuthData);
    return service == null ? createService((TokenAuthData) authData) : service;
  }

  private RememberTheMilkService createService(TokenAuthData authData) {
    return new RememberTheMilkService(
        new RememberTheMilkSignatureGenerator(appCredentials, authData.getToken()));
  }
}
