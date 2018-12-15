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
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskListModel;
import org.datatransferproject.types.common.models.tasks.TaskModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class GoogleTasksExporter implements Exporter<TokensAndUrlAuthData, TaskContainerResource> {

  private static final long PAGE_SIZE = 50; // TODO: configure correct size in production
  private final GoogleCredentialFactory credentialFactory;
  private final Monitor monitor;

  private volatile Tasks tasksClient;

  public GoogleTasksExporter(GoogleCredentialFactory credentialFactory, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.monitor = monitor;
  }

  @VisibleForTesting
  GoogleTasksExporter(
      GoogleCredentialFactory credentialFactory, Tasks tasksClient, Monitor monitor) {
    this(credentialFactory, monitor);
    this.tasksClient = tasksClient;
  }

  @Override
  public ExportResult<TaskContainerResource> export(
          UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    // Create a new tasks service for the authorized user
    Tasks tasksService = getOrCreateTasksService(authData);

    IdOnlyContainerResource resource =
        exportInformation.isPresent()
            ? (IdOnlyContainerResource) exportInformation.get().getContainerResource()
            : null;

    PaginationData paginationData =
        exportInformation.isPresent() ? exportInformation.get().getPaginationData() : null;

    try {
      if (resource != null) {
        return getTasks(tasksService, resource, Optional.ofNullable(paginationData));
      } else {
        return getTasksList(tasksService, Optional.ofNullable(paginationData));
      }
    } catch (Exception e) {
      monitor.severe(() -> "Error occurred trying to retrieve task", e);
      return new ExportResult<>(e);
    }
  }

  private ExportResult<TaskContainerResource> getTasks(
      Tasks tasksService, IdOnlyContainerResource resource, Optional<PaginationData> paginationData)
      throws IOException {
    Tasks.TasksOperations.List query =
        tasksService.tasks().list(resource.getId()).setMaxResults(PAGE_SIZE);

    if (paginationData.isPresent()) {
      query.setPageToken(((StringPaginationToken) paginationData.get()).getToken());
    }

    com.google.api.services.tasks.model.Tasks result = query.execute();
    List<TaskModel> newTasks =
        result
            .getItems()
            .stream()
            .map(
                t ->
                    new TaskModel(
                        resource.getId(),
                        t.getTitle(),
                        t.getNotes(),
                        t.getCompleted() != null
                            ? Instant.ofEpochMilli(t.getCompleted().getValue())
                            : null,
                        t.getDue() != null ? Instant.ofEpochMilli(t.getDue().getValue()) : null))
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

  private ExportResult<TaskContainerResource> getTasksList(
      Tasks tasksService, Optional<PaginationData> paginationData) throws IOException {
    Tasks.Tasklists.List query = tasksService.tasklists().list().setMaxResults(PAGE_SIZE);
    if (paginationData.isPresent()) {
      query.setPageToken(((StringPaginationToken) paginationData.get()).getToken());
    }
    TaskLists result = query.execute();
    ImmutableList.Builder<TaskListModel> newTaskListsBuilder = ImmutableList.builder();
    ImmutableList.Builder<IdOnlyContainerResource> newResourcesBuilder = ImmutableList.builder();

    for (TaskList taskList : result.getItems()) {
      newTaskListsBuilder.add(new TaskListModel(taskList.getId(), taskList.getTitle()));
      newResourcesBuilder.add(new IdOnlyContainerResource(taskList.getId()));
    }

    PaginationData newPage = null;
    ResultType resultType = ResultType.END;
    if (result.getNextPageToken() != null) {
      newPage = new StringPaginationToken(result.getNextPageToken());
      resultType = ResultType.CONTINUE;
    }

    List<IdOnlyContainerResource> newResources = newResourcesBuilder.build();
    if (!newResources.isEmpty()) {
      resultType = ResultType.CONTINUE;
    }

    TaskContainerResource taskContainerResource =
        new TaskContainerResource(newTaskListsBuilder.build(), null);
    ContinuationData continuationData = new ContinuationData(newPage);
    newResourcesBuilder.build().forEach(continuationData::addContainerResource);
    return new ExportResult<>(resultType, taskContainerResource, continuationData);
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
