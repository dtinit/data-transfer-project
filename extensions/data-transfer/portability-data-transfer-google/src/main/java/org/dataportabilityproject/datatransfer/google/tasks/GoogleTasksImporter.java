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

package org.dataportabilityproject.datatransfer.google.tasks;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.common.annotations.VisibleForTesting;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempTasksData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class GoogleTasksImporter implements Importer<TokensAndUrlAuthData, TaskContainerResource> {
  private final Logger logger = LoggerFactory.getLogger(GoogleTasksImporter.class);

  private final JobStore jobStore;
  private Tasks tasksClient;

  public GoogleTasksImporter(JobStore jobStore) {
    this(jobStore, null);
  }

  @VisibleForTesting
  public GoogleTasksImporter(JobStore jobStore, Tasks tasksClient) {
    this.jobStore = jobStore;
    this.tasksClient = tasksClient;
  }

  @Override
  public ImportResult importItem(
          UUID jobId, TokensAndUrlAuthData authData, TaskContainerResource data) {

    Tasks tasksService = getOrCreateTasksService(authData);
    TempTasksData tempTasksData = jobStore.findData(TempTasksData.class, jobId);
    if (tempTasksData == null) {
      tempTasksData = new TempTasksData(jobId.toString());
      jobStore.create(jobId, tempTasksData);
    }

    for (TaskListModel oldTasksList : data.getLists()) {
      // TempTasksData shouldn't be null since we added it.
      tempTasksData = jobStore.findData(TempTasksData.class, jobId);
      TaskList newTaskList = new TaskList().setTitle("Imported copy - " + oldTasksList.getName());
      TaskList insertedTaskList;

      try {
        insertedTaskList = tasksService.tasklists().insert(newTaskList).execute();
      } catch (IOException e) {
        return new ImportResult(ResultType.ERROR, "Error inserting taskList: " + e.getMessage());
      }

      tempTasksData.addTaskListId(oldTasksList.getId(), insertedTaskList.getId());

      jobStore.update(jobId, tempTasksData);
    }

    tempTasksData = jobStore.findData(TempTasksData.class, jobId);

    for (TaskModel oldTask : data.getTasks()) {
      Task newTask = new Task().setTitle(oldTask.getText()).setNotes(oldTask.getNotes());
      String newTaskId = tempTasksData.lookupNewTaskListId(newTask.getId());
      try {
        tasksService.tasks().insert(newTaskId, newTask).execute();
      } catch (IOException e) {
        return new ImportResult(ResultType.ERROR, "Error inserting task: " + e.getMessage());
      }
    }

    return new ImportResult(ResultType.OK);
  }

  private Tasks getOrCreateTasksService(TokensAndUrlAuthData authData) {
    return tasksClient == null ? makeTasksService(authData) : tasksClient;
  }

  private synchronized Tasks makeTasksService(TokensAndUrlAuthData authData) {
    Credential credential =
        new Credential(BearerToken.authorizationHeaderAccessMethod())
            .setAccessToken(authData.getAccessToken());
    return new Tasks.Builder(
            GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
