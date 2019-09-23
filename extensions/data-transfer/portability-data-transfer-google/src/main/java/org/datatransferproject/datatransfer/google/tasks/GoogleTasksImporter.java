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
package org.datatransferproject.datatransfer.google.tasks;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.common.annotations.VisibleForTesting;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.tasks.TaskContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskListModel;
import org.datatransferproject.types.common.models.tasks.TaskModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.UUID;


public class GoogleTasksImporter implements Importer<TokensAndUrlAuthData, TaskContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private Tasks tasksClient;

  public GoogleTasksImporter(GoogleCredentialFactory credentialFactory) {
    this(credentialFactory, null);
  }

  @VisibleForTesting
  GoogleTasksImporter(GoogleCredentialFactory credentialFactory,
      Tasks tasksClient) {
    this.credentialFactory = credentialFactory;
    this.tasksClient = tasksClient;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      TaskContainerResource data) throws Exception {
    Tasks tasksService = getOrCreateTasksService(authData);

    for (TaskListModel oldTasksList : data.getLists()) {
      TaskList newTaskList = new TaskList().setTitle("Imported copy - " + oldTasksList.getName());
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          oldTasksList.getId(),
          oldTasksList.getName(),
          () -> tasksService.tasklists().insert(newTaskList).execute().getId());
    }

    for (TaskModel oldTask : data.getTasks()) {
      Task newTask = new Task().setTitle(oldTask.getText()).setNotes(oldTask.getNotes());
      if (oldTask.getCompletedTime() != null) {
        newTask.setCompleted(new DateTime(oldTask.getCompletedTime().toEpochMilli()));
      }
      if (oldTask.getDueTime() != null) {
        newTask.setDue(new DateTime(oldTask.getDueTime().toEpochMilli()));
      }
      // If its not cached that means the task list create failed.
      if (idempotentImportExecutor.isKeyCached(oldTask.getTaskListId())) {
        String newTaskListId = idempotentImportExecutor.getCachedValue(oldTask.getTaskListId());
        idempotentImportExecutor.executeAndSwallowIOExceptions(
            oldTask.getTaskListId() + oldTask.getText(),
            oldTask.getText(),
            () -> tasksService.tasks().insert(newTaskListId, newTask).execute().getId());
      }
    }

    return new ImportResult(ResultType.OK);
  }

  private Tasks getOrCreateTasksService(TokensAndUrlAuthData authData) {
    return tasksClient == null ? makeTasksService(authData) : tasksClient;
  }

  private synchronized Tasks makeTasksService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Tasks.Builder(
        credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
