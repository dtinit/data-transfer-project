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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.dataportabilityproject.transfer.todoist.tasks.model.Project;
import org.dataportabilityproject.transfer.todoist.tasks.model.Task;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;

public class TodoistTasksService {

  private static final String BASE_URL = "https://beta.todoist.com/API/v8/";
  private final HttpTransport httpTransport;
  private final ObjectMapper objectMapper;
  private final TokensAndUrlAuthData authData;

  TodoistTasksService(HttpTransport httpTransport, ObjectMapper objectMapper,
      TokensAndUrlAuthData authData) {
    this.httpTransport = httpTransport;
    this.objectMapper = objectMapper;
    this.authData = authData;
  }

  List<Task> getTasksList() throws IOException {
    String url = BASE_URL + "tasks";
    Task[] result = makeRequest(url, Task[].class);
    return Arrays.asList(result);
  }

  List<Task> getTasksForProject(String projectId) throws IOException {
    String url = BASE_URL + "tasks?project_id=" + projectId;
    Task[] result = makeRequest(url, Task[].class);
    return Arrays.asList(result);
  }

  List<Project> getProjectsList() throws IOException {
    String url = BASE_URL + "projects";
    Project[] result = makeRequest(url, Project[].class);
    return Arrays.asList(result);
  }

  Project addProject(Project project) throws IOException {
    String url = BASE_URL + "projects";
    HttpContent content = new JsonHttpContent(new JacksonFactory(), project);
    return postRequest(url, content, Project.class);
  }

  Task addTask(Task task) throws IOException {
    String url = BASE_URL + "tasks";
    HttpContent content = new JsonHttpContent(new JacksonFactory(), task);
    return postRequest(url, content, Task.class);
  }

  private <T> T postRequest(String url,
      HttpContent content,
      Class<T> clazz) throws IOException {
    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + url;
    }

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest postRequest = requestFactory.buildPostRequest(new GenericUrl(fullUrl), content);

    HttpHeaders headers = new HttpHeaders();
    headers.setAuthorization("Bearer " + authData.getAccessToken());
    headers.setContentType("application/json");
    //headers.set("X-Request-Id", UUID.randomUUID()); // TODO: this feels wrong

    HttpResponse response = postRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    String result = CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  private <T> T makeRequest(String url, Class<T> clazz)
      throws IOException {
    // TODO: move this to a library
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(new GenericUrl(url));

    HttpHeaders headers = new HttpHeaders();
    headers.setAuthorization("Bearer " + authData.getAccessToken());
    getRequest.setHeaders(headers);

    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }
}
