/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.tasks.TaskListModel;
import org.dataportabilityproject.dataModels.tasks.TaskModel;
import org.dataportabilityproject.dataModels.tasks.TaskModelWrapper;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.GetListResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.GetListsResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.ListAddResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.ListInfo;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.RememberTheMilkResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.Task;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskAddResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskList;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskSeries;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TimelineCreateResponse;
import org.dataportabilityproject.shared.IdOnlyResource;

/**
 * List the lists that exist in RTM.
 */
final class RememberTheMilkTaskService implements
    Importer<TaskModelWrapper>,
    Exporter<TaskModelWrapper> {

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private final RememberTheMilkSignatureGenerator signatureGenerator;
  private final JobDataCache jobDataCache;

  RememberTheMilkTaskService(RememberTheMilkSignatureGenerator signatureGenerator,
      JobDataCache jobDataCache) throws IOException {
    this.signatureGenerator = signatureGenerator;
    this.jobDataCache = jobDataCache;
  }

  private GetListsResponse getLists() throws IOException {
    return makeRequest(RememberTheMilkMethods.LISTS_GET_LIST, ImmutableMap.of(),
        GetListsResponse.class);
  }

  private GetListResponse getList(int id) throws IOException {
    return makeRequest(RememberTheMilkMethods.TASKS_GET_LIST,
        ImmutableMap.of("list_id", Long.toString(id)),
        GetListResponse.class);
  }

  @Override
  public void importItem(TaskModelWrapper wrapper) throws IOException {
    String timeline = createTimeline();
    for (TaskListModel taskList : wrapper.getLists()) {
      ListInfo listInfo = createTaskList(taskList.getName(), timeline);
      jobDataCache.store(taskList.getId(), listInfo.id);
    }
    for (TaskModel task : wrapper.getTasks()) {
      int newList = jobDataCache.getData(task.getTaskListId(), Integer.class);
      TaskSeries addedTask = createTask(
          task.getText(), timeline, newList);
      // TODO add note here
    }
  }

  @Override
  public TaskModelWrapper export(ExportInformation exportInformation) throws IOException {
    if (exportInformation.getResource().isPresent()) {
      return exportTaskList(
          exportInformation.getResource().get(),
          exportInformation.getPaginationInformation());
    } else {
      return exportTaskLists(exportInformation.getPaginationInformation());
    }
  }

  private TaskModelWrapper exportTaskLists(
      Optional<PaginationInformation> paginationInformation) throws IOException {
    List<TaskListModel> lists = new ArrayList<>();
    List<Resource> subResources = new ArrayList<>();
    for (ListInfo oldListInfo : getLists().listInfoList.lists) {
      if (oldListInfo.name.equals("All Tasks")) {
        // All Tasks is a special list that contains everything,
        // don't copy that over.
        continue;
      }
      lists.add(new TaskListModel(
          Integer.toString(oldListInfo.id),
          oldListInfo.name));
      subResources.add(new IdOnlyResource(Integer.toString(oldListInfo.id)));
    }
    return new TaskModelWrapper(lists, null, new ContinuationInformation(subResources, null));
  }

  private TaskModelWrapper exportTaskList(
      Resource resource,
      Optional<PaginationInformation> paginationInformation) throws IOException {
    int oldListId = Integer.parseInt(((IdOnlyResource) resource).getId());
    GetListResponse oldList = getList(oldListId);
    List<TaskList> taskLists = oldList.tasks.list;
    List<TaskModel> tasks = new ArrayList<>();
    for (TaskList taskList : taskLists) {
      if (taskList.taskSeriesList != null) {
        for (TaskSeries taskSeries : taskList.taskSeriesList) {
          tasks.add(new TaskModel(
              Integer.toString(oldListId),
              taskSeries.name,
              taskSeries.notes.toString()));
          for (Task task : taskSeries.tasks) {
            // Do something here with completion date, but its odd there can be more than one.
          }
        }
      }
    }
    return new TaskModelWrapper(null, tasks, null);
  }

  private String createTimeline() throws IOException {
    TimelineCreateResponse timelineCreateResponse =
        makeRequest(RememberTheMilkMethods.TIMELINES_CREATE, ImmutableMap.of(),
            TimelineCreateResponse.class);
    checkState(!Strings.isNullOrEmpty(timelineCreateResponse.timeline));
    return timelineCreateResponse.timeline;
  }

  private ListInfo createTaskList(String name, String timeline) throws IOException {
    Map<String, String> params = ImmutableMap.of(
        "timeline", timeline,
        "name", ("Copy of: " + name)
    );
    ListAddResponse response = makeRequest(RememberTheMilkMethods.LISTS_ADD, params,
        ListAddResponse.class);
    checkState(response.listInfo != null, "Added list is null");
    checkState(response.listInfo.id != 0, "Added list has id of zero");
    return response.listInfo;
  }

  private TaskSeries createTask(String name, String timeline, long listId) throws IOException {
    Map<String, String> params = ImmutableMap.of(
        "timeline", timeline,
        "name", name,
        "list_id", Long.toString(listId)
    );
    TaskAddResponse taskAddResponse = makeRequest(RememberTheMilkMethods.TASK_ADD, params,
        TaskAddResponse.class);
    return taskAddResponse.taskList.taskSeriesList.get(0);
  }

  private <T extends RememberTheMilkResponse> T makeRequest(RememberTheMilkMethods method,
      Map<String, String> parameters,
      Class<T> dataClass) throws IOException {

    StringBuilder parameterString = new StringBuilder();
    for (String key : parameters.keySet()) {
      parameterString
          .append("&")
          .append(key)
          .append("=")
          .append(parameters.get(key));
    }

    URL url = new URL(method.getUrl() + parameterString);
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    T parsedResponse = response.parseAs(dataClass);

    if (parsedResponse.error != null) {
      throw new IOException(
          "Error making call to " + signedUrl + " error: " + parsedResponse.error);
    }

    return parsedResponse;
  }

}
