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
package org.dataportabilityproject.transfer.rememberthemilk.model.tasks;

import com.fasterxml.jackson.xml.annotate.JacksonXmlElementWrapper;
import com.fasterxml.jackson.xml.annotate.JacksonXmlProperty;
import com.google.api.client.util.Joiner;
import com.google.api.client.util.Key;

import java.util.ArrayList;
import java.util.List;

/** A tasks series, see: https://www.rememberthemilk.com/services/api/tasks.rtm */
public class TaskSeries {

  @JacksonXmlProperty(isAttribute = true, localName = "id")
  public int id;

  @JacksonXmlProperty(isAttribute = true, localName = "created")
  public String created;

  @JacksonXmlProperty(isAttribute = true, localName = "modified")
  public String modified;

  @JacksonXmlProperty(isAttribute = true, localName = "name")
  public String name;

  @JacksonXmlProperty(isAttribute = true, localName = "source")
  public String source;

  @JacksonXmlProperty(isAttribute = true, localName = "location_id")
  public String location_id;

  @JacksonXmlProperty(localName = "url")
  public String url;

  @JacksonXmlProperty(localName = "tags")
  public String tags;

  @JacksonXmlProperty(localName ="participants")
  public String participants;

  @JacksonXmlProperty(localName ="notes")
  public Notes notes;

  @JacksonXmlProperty(localName ="tasks")
  public List<Task> tasks;

  // Either tasks or task will be present - not both
  @JacksonXmlProperty(localName = "task")
  public Task task;

  @Override
  public String toString() {
    return String.format(
        "TaskSeries(id=%d, created=%s, modified=%s, name=%s, source=%s, notes=%s, tasks:%s)",
        id,
        created,
        modified,
        name,
        source,
        notes,
        (tasks == null || tasks.isEmpty()) ? "" : Joiner.on('\n').join(tasks));
  }
}
