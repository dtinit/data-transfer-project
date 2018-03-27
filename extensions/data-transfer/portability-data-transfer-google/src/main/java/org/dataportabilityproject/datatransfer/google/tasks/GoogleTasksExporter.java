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
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;

public class GoogleTasksExporter implements Exporter<TokensAndUrlAuthData, TaskContainerResource> {
  private static final long PAGE_SIZE = 50;
  private volatile Tasks tasksClient;

  public GoogleTasksExporter() {
    this.tasksClient = null;
  }

  @VisibleForTesting
  GoogleTasksExporter(Tasks tasksClient) {
    this.tasksClient = tasksClient;
  }

  @Override
  public ExportResult<TaskContainerResource> export(TokensAndUrlAuthData authData) {
    return export(authData, new ExportInformation(null, null));
  }

  @Override
  public ExportResult<TaskContainerResource> export(
      TokensAndUrlAuthData authData, ExportInformation exportInformation) {
    // Create a new tasks service for the authorized user
    Tasks tasksService = getOrCreateTasksService(authData);

    IdOnlyContainerResource resource =
        (IdOnlyContainerResource) exportInformation.getContainerResource();

    try {
      if (resource != null) {
        return getTasks(tasksService, resource, exportInformation.getPaginationData());
      } else {
        return getTasksList(tasksService, exportInformation.getPaginationData());
      }
    } catch (Exception e) {
      return new ExportResult<>(ResultType.ERROR, "Error retrieving tasks: " + e.getMessage());
    }
  }

  private ExportResult getTasks(
      Tasks tasksService, IdOnlyContainerResource resource, PaginationData paginationData)
      throws IOException {
    Tasks.TasksOperations.List query =
        tasksService.tasks().list(resource.getId()).setMaxResults(PAGE_SIZE);

    if (paginationData != null) {
      query.setPageToken(((StringPaginationToken) paginationData).getToken());
    }

    com.google.api.services.tasks.model.Tasks result = query.execute();
    List<TaskModel> newTasks =
        result
            .getItems()
            .stream()
            .map(t -> new TaskModel(resource.getId(), t.getTitle(), t.getNotes()))
            .collect(Collectors.toList());

    PaginationData newPage = null;
    ResultType resultType = ResultType.END;
    if (result.getNextPageToken() != null) {
      newPage = new StringPaginationToken(result.getNextPageToken());
      resultType = ResultType.CONTINUE;
    }

    TaskContainerResource taskContainerResource = new TaskContainerResource(null, newTasks);

    return new ExportResult<>(resultType, taskContainerResource, new ContinuationData(newPage));
  }

  private ExportResult getTasksList(Tasks tasksSerivce, PaginationData paginationData) throws IOException {
    Tasks.Tasklists.List query = tasksSerivce.tasklists().list().setMaxResults(PAGE_SIZE);
    if (paginationData != null) {
      query.setPageToken(((StringPaginationToken) paginationData).getToken());
    }
    TaskLists result = query.execute();
    ImmutableList.Builder<TaskListModel> newTaskListsBuilder = ImmutableList.builder();
    ImmutableList.Builder<IdOnlyContainerResource> newResourcesBuilder = ImmutableList.builder();

    for(TaskList taskList : result.getItems()) {
      newTaskListsBuilder.add(new TaskListModel(taskList.getId(), taskList.getTitle()));
      newResourcesBuilder.add(new IdOnlyContainerResource(taskList.getId()));
    }

    PaginationData newPage = null;
    ResultType resultType = ResultType.END;
    if(result.getNextPageToken()!=null) {
      newPage = new StringPaginationToken(result.getNextPageToken());
      resultType = ResultType.CONTINUE;
    }

    List<IdOnlyContainerResource> newResources = newResourcesBuilder.build();
    if(!newResources.isEmpty()) { resultType = ResultType.CONTINUE; }

    TaskContainerResource taskContainerResource = new TaskContainerResource(newTaskListsBuilder.build(), null);
    ContinuationData continuationData = new ContinuationData(newPage);
    newResourcesBuilder.build().forEach(resource -> continuationData.addContainerResource(resource));
    return new ExportResult<>(resultType,taskContainerResource, continuationData);
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
