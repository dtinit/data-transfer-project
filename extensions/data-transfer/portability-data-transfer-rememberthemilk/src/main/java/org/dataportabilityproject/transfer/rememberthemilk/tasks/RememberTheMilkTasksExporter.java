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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.transfer.rememberthemilk.RememberTheMilkSignatureGenerator;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.GetListResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.Task;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskList;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;

/*
 * Exporter for Tasks data type from Remember The Milk Service.
 */
public class RememberTheMilkTasksExporter implements Exporter<AuthData, TaskContainerResource> {
  private final AppCredentials appCredentials;
  private RememberTheMilkService service;

  public RememberTheMilkTasksExporter(AppCredentials appCredentials) {
    this.appCredentials = appCredentials;
    this.service = null;
  }

  @VisibleForTesting
  public RememberTheMilkTasksExporter(
      AppCredentials appCredentials, RememberTheMilkService service) {
    this.appCredentials = appCredentials;
    this.service = service;
  }

  @Override
  public ExportResult<TaskContainerResource> export(UUID jobId, AuthData authData) {
    return export(jobId, authData, new ExportInformation(null, null));
  }

  @Override
  public ExportResult<TaskContainerResource> export(
      UUID jobId, AuthData authData, ExportInformation exportInformation) {
    // Create new service for the authorized user
    RememberTheMilkService service = getOrCreateService(authData);

    IdOnlyContainerResource resource =
        (IdOnlyContainerResource) exportInformation.getContainerResource();
    if (resource != null) {
      return exportTask(service, resource);
    } else {
      return exportTaskList(service);
    }
  }

  private ExportResult exportTask(
      RememberTheMilkService service, IdOnlyContainerResource resource) {
    String oldListId = resource.getId();
    GetListResponse oldList = null;
    try {
      oldList = service.getList(oldListId);
    } catch (IOException e) {
      return new ExportResult(ResultType.ERROR, "Error getting old list: " + e.getMessage());
    }

    List<TaskList> taskLists = oldList.tasks.list;
    List<TaskModel> tasks = new ArrayList<>();

    for (TaskList taskList : taskLists) {
      if (taskList.taskSeriesList != null) {
        for (TaskSeries taskSeries : taskList.taskSeriesList) {
          tasks.add(new TaskModel(oldListId, taskSeries.name, taskSeries.notes.toString()));
          for (Task task : taskSeries.tasks) {
            // Do something here with completion date, but its odd there can be more than one.
          }
        }
      }
    }

    TaskContainerResource taskContainerResource = new TaskContainerResource(null, tasks);
    // TODO: what do we do with pagination data?
    return new ExportResult(ResultType.CONTINUE, taskContainerResource, null);
  }

  private ExportResult exportTaskList(RememberTheMilkService service) {
    List<TaskListModel> lists = new ArrayList<>();
    List<IdOnlyContainerResource> subResources = new ArrayList<>();

    List<ListInfo> listInfoList;
    try {
      listInfoList = service.getLists().listInfoList.lists;
    } catch (IOException e) {
      return new ExportResult(ResultType.ERROR, "Error retrieving lists: " + e.getMessage());
    }

    for (ListInfo oldListInfo : listInfoList) {
      if (oldListInfo.name.equals("All Tasks")) {
        // All Tasks is a special list that contains everything, don't copy that over
        continue;
      }
      lists.add(new TaskListModel(Integer.toString(oldListInfo.id), oldListInfo.name));
      subResources.add(new IdOnlyContainerResource(Integer.toString(oldListInfo.id)));
    }

    TaskContainerResource taskContainerResource = new TaskContainerResource(lists, null);
    ContinuationData continuationData = new ContinuationData(null);
    subResources.forEach(resource -> continuationData.addContainerResource(resource));
    // TODO: what do we do with pagination data?
    return new ExportResult(ResultType.CONTINUE, taskContainerResource, continuationData);
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
