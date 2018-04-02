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
import java.util.Map;
import org.dataportabilityproject.transfer.rememberthemilk.RememberTheMilkSignatureGenerator;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListAddResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.ListInfo;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.RememberTheMilkResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskAddResponse;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.dataportabilityproject.transfer.rememberthemilk.model.tasks.TimelineCreateResponse;

class RememberTheMilkService {
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private RememberTheMilkSignatureGenerator signatureGenerator;

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
    checkState(response.listInfo != null, "Added list is null");
    checkState(response.listInfo.id != 0, "Added list has id of zero");
    return response.listInfo;
  }

  public TaskSeries createTask(String name, String timeline, String listId) throws IOException {
    Map<String, String> params =
        ImmutableMap.of("timeline", timeline, "name", name, "list_id", listId);
    TaskAddResponse taskAddResponse =
        makeRequest(RememberTheMilkMethods.TASK_ADD, params, TaskAddResponse.class);
    return taskAddResponse.taskList.taskSeriesList.get(0);
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

  public enum RememberTheMilkMethods {
    CHECK_TOKEN("rtm.auth.checkToken"),
    GET_FROB("rtm.auth.getFrob"),
    LISTS_GET_LIST("rtm.lists.getList"),
    LISTS_ADD("rtm.lists.add"),
    GET_TOKEN("rtm.auth.getToken"),
    TASKS_GET_LIST("rtm.tasks.getList"),
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
