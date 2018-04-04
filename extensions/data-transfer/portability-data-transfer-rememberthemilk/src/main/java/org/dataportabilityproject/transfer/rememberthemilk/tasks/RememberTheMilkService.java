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
import com.google.api.client.util.IOUtils;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.GetListResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.GetListsResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListAddResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.RememberTheMilkResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskAddResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TimelineCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RememberTheMilkService {
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private RememberTheMilkSignatureGenerator signatureGenerator;
  private Logger logger = LoggerFactory.getLogger(RememberTheMilkService.class);
  private XmlMapper xmlMapper = new XmlMapper();

  RememberTheMilkService(RememberTheMilkSignatureGenerator signatureGenerator) {
    this.signatureGenerator = signatureGenerator;
  }

  public String createTimeline() throws IOException {
    TimelineCreateResponse timelineCreateResponse =
        makeRequest(
            RememberTheMilkMethods.TIMELINES_CREATE,
            ImmutableMap.of(),
            TimelineCreateResponse.class);
    checkState(!Strings.isNullOrEmpty(timelineCreateResponse.timeline));
    return timelineCreateResponse.timeline;
  }

  public ListInfo createTaskList(String name, String timeline) throws IOException {
    Map<String, String> params =
        ImmutableMap.of("timeline", timeline, "name", ("Copy of: " + name));
    ListAddResponse response =
        makeRequest(RememberTheMilkMethods.LISTS_ADD, params, ListAddResponse.class);
    checkState(response.list != null, "Added list is null");
    checkState(response.list.id != 0, "Added list has id of zero");
    return response.list;
  }

  public TaskSeries createTask(String name, String timeline, String listId) throws IOException {
    Map<String, String> params =
        ImmutableMap.of("timeline", timeline, "name", name, "list_id", listId);
    TaskAddResponse taskAddResponse =
        makeRequest(RememberTheMilkMethods.TASK_ADD, params, TaskAddResponse.class);
    return taskAddResponse.list.taskseries.get(0);
  }

  public GetListResponse getList(String listId) throws IOException {
    Map<String, String> params = ImmutableMap.of("list_id", listId);
    return makeRequest(RememberTheMilkMethods.LISTS_GET_LIST, params, GetListResponse.class);
  }

  public GetListsResponse getLists() throws IOException {
    return makeRequest(
        RememberTheMilkMethods.LISTS_GET_LIST, ImmutableMap.of(), GetListsResponse.class);
  }

  private <T extends RememberTheMilkResponse> T makeRequest(
      RememberTheMilkMethods method, Map<String, String> parameters, Class<T> dataClass)
      throws IOException {

    StringBuilder parameterString = new StringBuilder();
    for (String key : parameters.keySet()) {
      parameterString.append("&").append(key).append("=").append(parameters.get(key));
    }

    URL url = new URL(method.getUrl() + parameterString);
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    IOUtils.copy(response.getContent(), bos, true);
    String content = bos.toString();
    logger.debug("Content: {}", content);
    T parsedResponse = xmlMapper.readValue(content, dataClass);

    if (parsedResponse.error != null) {
      throw new IOException(
          "Error making call to " + signedUrl + " error: " + parsedResponse.error);
    }

    return parsedResponse;
  }

  public enum RememberTheMilkMethods {
    LISTS_GET_LIST("rtm.lists.getList"),
    LISTS_ADD("rtm.lists.add"),
    TASK_ADD("rtm.tasks.add"),
    TIMELINES_CREATE("rtm.timelines.create");

    private static final String BASE_URL = "https://api.rememberthemilk.com/services/rest/";
    private final String methodName;

    RememberTheMilkMethods(String methodName) {
      this.methodName = methodName;
    }

    String getMethodName() {
      return methodName;
    }

    String getUrl() {
      return BASE_URL + "?method=" + getMethodName();
    }
  }
}
