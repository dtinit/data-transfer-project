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
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.tasks.TaskContainerResource;
import org.dataportabilityproject.types.transfer.models.tasks.TaskListModel;
import org.dataportabilityproject.types.transfer.models.tasks.TaskModel;

public class RememberTheMilkTasksExporter implements Exporter<AuthData, TaskContainerResource> {
  private final RememberTheMilkService service;

  public RememberTheMilkTasksExporter(RememberTheMilkSignatureGenerator signatureGenerator) {
    service = new RememberTheMilkService(signatureGenerator);
  }

  @Override
  public ExportResult<TaskContainerResource> export(UUID jobId, AuthData authData) {
    return export(jobId, authData, new ExportInformation(null, null));
  }

  @Override
  public ExportResult<TaskContainerResource> export(
      UUID jobId, AuthData authData, ExportInformation exportInformation) {
    IdOnlyContainerResource resource =
        (IdOnlyContainerResource) exportInformation.getContainerResource();
    if (resource != null) {
      return exportTask(resource);
    } else {
      return exportTaskList();
    }
  }

  private ExportResult exportTask(IdOnlyContainerResource resource) {
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

  private ExportResult exportTaskList() {
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
}
