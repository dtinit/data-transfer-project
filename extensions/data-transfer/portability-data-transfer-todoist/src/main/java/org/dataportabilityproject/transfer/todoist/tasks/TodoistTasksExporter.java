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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.transfer.todoist.tasks.model.Due;
import org.dataportabilityproject.transfer.todoist.tasks.model.Project;
import org.dataportabilityproject.transfer.todoist.tasks.model.Task;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoistTasksExporter implements Exporter<TokensAndUrlAuthData, TaskContainerResource> {

  private final Logger logger = LoggerFactory.getLogger(TodoistTasksExporter.class);
  private HttpTransport httpTransport;
  private ObjectMapper objectMapper;
  private volatile TodoistTasksService service;

  public TodoistTasksExporter(ObjectMapper objectMapper, HttpTransport httpTransport) {
    this.objectMapper = objectMapper;
    this.httpTransport = httpTransport;
    this.service = null;
  }

  @VisibleForTesting
  TodoistTasksExporter(TodoistTasksService service) {
    this.service = service;
  }

  @Override
  public ExportResult<TaskContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) {
    if (!exportInformation.isPresent()) {
      return exportProjects(authData, Optional.empty());
    } else {
      PaginationData paginationData = exportInformation.get().getPaginationData();
      if (paginationData != null) {
        return null;
      } else {
        // Next thing to export is tasks
        IdOnlyContainerResource idOnlyContainerResource = (IdOnlyContainerResource) exportInformation
            .get().getContainerResource();
        Optional<PaginationData> pageData = Optional.ofNullable(paginationData);
        return exportTasks(authData, idOnlyContainerResource.getId(), pageData);
      }
    }
  }

  private ExportResult<TaskContainerResource> exportProjects(AuthData authData,
      Optional<PaginationData> paginationData) {
    // TODO: Todoist does not support pagination, so we have to keep that in mind
    Preconditions.checkNotNull(authData);
    service = getOrCreateService(authData);

    // Get project information
    List<Project> projectList;
    try {
      projectList = getOrCreateService(authData).getProjectsList();
      logger.debug("Project list: " + projectList);
    } catch (IOException e) {
      return new ExportResult(ResultType.ERROR, "Error retrieving projects: " + e.getMessage());
    }

    // Set up continuation data
    PaginationData nextPageData = null;
    ContinuationData continuationData = new ContinuationData(nextPageData);

    // Process project list
    List<TaskListModel> taskListModels = new ArrayList<>(projectList.size());
    for (Project project : projectList) {
      TaskListModel model = convertToTaskListModel(project);
      continuationData.addContainerResource(new IdOnlyContainerResource(project.getId()));
      taskListModels.add(model);
    }
    TaskContainerResource taskContainerResource = new TaskContainerResource(taskListModels, null);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (taskListModels.isEmpty()) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, taskContainerResource, continuationData);
  }

  private ExportResult<TaskContainerResource> exportTasks(AuthData authData, String listId,
      Optional<PaginationData> pageData) {
    List<Task> taskList;

    // Get task information
    try {
      taskList = getOrCreateService(authData).getTasksForProject(listId);
    } catch (IOException e) {
      return new ExportResult(ResultType.ERROR, "Error retrieving tasks: " + e.getMessage());
    }

    // Set up continuation data
    PaginationData nextPageData = null;
    ContinuationData continuationData = new ContinuationData(nextPageData);

    // Process task list
    List<TaskModel> taskModels = new ArrayList<>(taskList.size());
    for (Task task : taskList) {
      TaskModel model = convertToTaskModel(task);
      taskModels.add(model);
    }
    TaskContainerResource taskContainerResource = new TaskContainerResource(null, taskModels);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, taskContainerResource, continuationData);
  }

  private static TaskListModel convertToTaskListModel(Project project) {
    return new TaskListModel(project.getId(), project.getName());
  }

  private static TaskModel convertToTaskModel(Task task) {
    Instant dueTime = null;
    Due due = task.getDue();
    if (due.getDate() != null) {
      // TODO: address time zone in Due object?
      if (due.getDateTime() != null) {
        dueTime = Instant.parse(due.getDateTime());
      } else {
        LocalDate date = LocalDate.parse(due.getDate());
        if (due.getTimezone() != null) {
          dueTime = date.atStartOfDay(ZoneId.of(due.getTimezone())).toInstant();
        } else {
          // TODO: deal with this!  This is not the right thing to do!
          dueTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
      }
    }
    return new TaskModel(task.getProjectId(),
        task.getContent(),
        null,  // TODO: address notes
        task.getCompleted(),
        null,
        dueTime);
  }

  private TodoistTasksService getOrCreateService(AuthData authData) {
    Preconditions.checkArgument(authData instanceof TokensAndUrlAuthData);
    return service == null ? createService((TokensAndUrlAuthData) authData) : service;
  }

  private TodoistTasksService createService(TokensAndUrlAuthData authData) {
    return new TodoistTasksService(httpTransport, objectMapper, authData);
  }
}
