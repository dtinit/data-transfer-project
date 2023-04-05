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

package org.datatransferproject.transfer.rememberthemilk.model.tasks;

import com.fasterxml.jackson.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class ModelTest {
  private XmlMapper mapper = new XmlMapper();

  @Test
  public void parseListAddResponse() throws IOException {
    String content =
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<rsp stat=\"ok\">"
            + "<transaction id=\"123\" undoable=\"0\"/>"
            + "<list id=\"456\" name=\"list\" deleted=\"0\" locked=\"0\" "
            + "archived=\"0\" position=\"0\" smart=\"0\" sort_order=\"0\"/></rsp>\n";

    ListAddResponse response = mapper.readValue(content, ListAddResponse.class);
    assertThat(response.stat).isEqualTo("ok");
    assertThat(response.list.id).isEqualTo(456);
  }

  @Test
  public void parseTaskAddResponse() throws IOException {
    String content =
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<rsp stat=\"ok\">"
            + "  <transaction id=\"123\" undoable=\"0\"/>"
            + "  <list id=\"1234\">\n"
            + "    <taskseries id=\"1234\" created=\"2015-05-07T10:19:54Z\" modified=\"2015-05-07T10:19:54Z\"\n"
            + "             name=\"Get Bananas\" source=\"api\">"
            + "      <tags/>\n"
            + "      <participants/>\n"
            + "      <notes/>\n"
            // The taskSeries in a TaskUpdateResponse only contains one task, not a list of Tasks
            + "      <task id=\"123456789\" due=\"\" has_due_time=\"0\" added=\"2015-05-07T10:19:54Z\"\n"
            + "        completed=\"\" deleted=\"\" priority=\"N\" postponed=\"0\" estimate=\"\"/>\n"
            + "    </taskseries>\n"
            + "  </list>"
            + "</rsp>\n";

    TaskUpdateResponse taskUpdateResponse = mapper.readValue(content, TaskUpdateResponse.class);

    assertThat(taskUpdateResponse.stat).isEqualTo("ok");
  }

  @Test
  public void parseTask() throws IOException {
    String content =
        "<task id=\"123456789\" due=\"\" has_due_time=\"0\" added=\"2015-05-07T10:19:54Z\"\n"
            + "completed=\"\" deleted=\"\" priority=\"N\" postponed=\"0\" estimate=\"\"/>\n";

    Task task = mapper.readValue(content, Task.class);
    assertThat(task.id).isEqualTo(123456789);
  }

  @Test
  public void parseTaskSeries() throws IOException {
    String taskSeriesContent =
        "<taskseries id=\"1234\" created=\"2015-05-07T10:19:54Z\" modified=\"2015-05-07T10:19:54Z\"\n"
            + "      name=\"Get Bananas\" source=\"api\">"
            + "      <tags/>\n"
            + "      <participants/>\n"
            + "      <notes/>\n"
            + "      <task id=\"123456789\" due=\"\" has_due_time=\"0\" added=\"2015-05-07T10:19:54Z\"\n"
            + "        completed=\"\" deleted=\"\" priority=\"N\" postponed=\"0\" estimate=\"\"/>\n"
            + "</taskseries>\n";

    TaskSeries taskSeries = mapper.readValue(taskSeriesContent, TaskSeries.class);
    assertThat(taskSeries.tasks).isNotNull();
    assertThat(taskSeries.tasks.size()).isEqualTo(1);
    assertThat(taskSeries.tasks.get(0).id).isEqualTo(123456789);
  }

  @Test
  public void parseError() throws IOException {
    String content =
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<rsp stat=\"fail\">"
            + "  <err code=\"96\" msg=\"Invalid signature\"/>"
            + "</rsp>\n";

    // try to parse it as a specific response (which extends RmbrTheMilkResponse
    ListAddResponse listAddResponse = mapper.readValue(content, ListAddResponse.class);
  }

  @Test
  public void parseGetListsResponse() throws IOException {
    String content =
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<rsp stat=\"ok\">"
            + "  <lists>"
            + "    <list id=\"123\" name=\"Inbox\" deleted=\"0\" locked=\"1\" archived=\"0\" position=\"-1\" smart=\"0\" sort_order=\"0\"/>"
            + "    <list id=\"43132027\" name=\"Sent\" deleted=\"0\" locked=\"1\" archived=\"0\" position=\"1\" smart=\"0\" sort_order=\"0\"/>"
            + "    <list id=\"43132028\" name=\"Personal\" deleted=\"0\" locked=\"0\" archived=\"0\" position=\"0\" smart=\"0\" sort_order=\"0\"/>"
            + "    <list id=\"43132029\" name=\"Work\" deleted=\"0\" locked=\"0\" archived=\"0\" position=\"0\" smart=\"0\" sort_order=\"0\"/>"
            + "   </lists>"
            + "</rsp>";

    GetListsResponse response = mapper.readValue(content, GetListsResponse.class);

    assertThat(response.lists.size()).isEqualTo(4);
  }

  @Test
  public void parseGetListResponse() throws IOException {
    String content =
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<rsp stat=\"ok\">"
            + "  <tasks rev=\"abcdefg\">"
            + "    <list id=\"1234\"/>"
            + "    <list id=\"5678\"/>"
            + "  </tasks>"
            + "</rsp>\n";

    GetListResponse getListResponse = mapper.readValue(content, GetListResponse.class);
    assertThat(getListResponse.tasks.list.size()).isEqualTo(2);
  }
}
