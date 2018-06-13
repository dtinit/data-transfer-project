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

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.xml.XmlMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.print.DocFlavor.STRING;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.GetListResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.GetListsResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListAddResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.RememberTheMilkResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskUpdateResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TimelineCreateResponse;

class RememberTheMilkService {
  private static final String BASE_URL = "https://api.rememberthemilk.com/services/rest/";
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private RememberTheMilkSignatureGenerator signatureGenerator;
  private XmlMapper xmlMapper = new XmlMapper();

  RememberTheMilkService(RememberTheMilkSignatureGenerator signatureGenerator) {
    this.signatureGenerator = signatureGenerator;
  }

  public String createTimeline() throws IOException {
    TimelineCreateResponse timelineCreateResponse =
        makeRequest(
            ImmutableMap.of("method", RememberTheMilkMethods.TIMELINES_CREATE.getMethodName()),
            TimelineCreateResponse.class);
    checkState(!Strings.isNullOrEmpty(timelineCreateResponse.timeline));
    return timelineCreateResponse.timeline;
  }

  public ListInfo createTaskList(String name, String timeline) throws IOException {
    Map<String, String> params =
        ImmutableMap.of(
            "method",
            RememberTheMilkMethods.LISTS_ADD.getMethodName(),
            "timeline",
            timeline,
            "name",
            ("Copy of: " + name));
    ListAddResponse response = makeRequest(params, ListAddResponse.class);
    checkState(response.list != null, "Added list is null");
    checkState(response.list.id != 0, "Added list has id of zero");
    return response.list;
  }

  public TaskSeries createTask(String name, String timeline, String listId) throws IOException {
    Map<String, String> params =
        ImmutableMap.of(
            "method",
            RememberTheMilkMethods.TASKS_ADD.getMethodName(),
            "timeline",
            timeline,
            "name",
            name,
            "list_id",
            listId);
    TaskUpdateResponse taskUpdateResponse = makeRequest(params, TaskUpdateResponse.class);
    return taskUpdateResponse.list.taskseries.get(0);
  }

  public void completeTask(String timeline, String listId, int seriesId, int taskId)
      throws IOException {
    // NB: The RTM API does not support setting an arbitrary completion time, so this method can
    // only mark a task as having been completed.
    Map<String, String> params =
        ImmutableMap.of(
            "method",
            RememberTheMilkMethods.TASKS_COMPLETE.getMethodName(),
            "timeline",
            timeline,
            "list_id",
            listId,
            "taskseries_id",
            String.valueOf(seriesId),
            "task_id",
            String.valueOf(taskId)
        );
    makeRequest(params, TaskUpdateResponse.class);
  }

  public void setDueDate(String timeline, String listId, int seriesId, int taskId, Instant dueDate)
      throws IOException {
    // NB: does not set due time, merely due date
    // TODO: address due times
    Map<String, String> params = new LinkedHashMap<>();
    params.put("method", RememberTheMilkMethods.TASKS_DUE_DATE.getMethodName());
    params.put("timeline", timeline);
    params.put("list_id", listId);
    params.put("taskseries_id", String.valueOf(seriesId));
    params.put("task_id", String.valueOf(taskId));
    params.put("due", dueDate.toString());
    makeRequest(params, TaskUpdateResponse.class);
  }

  public GetListResponse getList(String listId) throws IOException {
    Map<String, String> params =
        ImmutableMap.of(
            "method", RememberTheMilkMethods.TASKS_GET_LIST.getMethodName(), "list_id", listId);
    return makeRequest(params, GetListResponse.class);
  }

  public GetListsResponse getLists() throws IOException {
    return makeRequest(
        ImmutableMap.of("method", RememberTheMilkMethods.LISTS_GET_LIST.getMethodName()),
        GetListsResponse.class);
  }

  private <T extends RememberTheMilkResponse> T makeRequest(
      Map<String, String> parameters, Class<T> dataClass) throws IOException {

    URL signedUrl = signatureGenerator.getSignature(BASE_URL, parameters);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    T parsedResponse = xmlMapper.readValue(response.getContent(), dataClass);

    if (parsedResponse.error != null) {
      throw new IOException(
          "Error making call to " + signedUrl + " error: " + parsedResponse.error);
    }

    return parsedResponse;
  }

  private enum RememberTheMilkMethods {
    LISTS_GET_LIST("rtm.lists.getList"),
    LISTS_ADD("rtm.lists.add"),
    TASKS_ADD("rtm.tasks.add"),
    TASKS_COMPLETE("rtm.tasks.complete"),
    TASKS_DUE_DATE("rtm.tasks.setDueDate"),
    TASKS_GET_LIST("rtm.tasks.getList"),
    TIMELINES_CREATE("rtm.timelines.create");

    private final String methodName;

    RememberTheMilkMethods(String methodName) {
      this.methodName = methodName;
    }

    String getMethodName() {
      return methodName;
    }
  }
}
