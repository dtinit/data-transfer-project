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

import com.fasterxml.jackson.xml.annotate.JacksonXmlProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  @JacksonXmlProperty(localName = "participants")
  public String participants;

  @JacksonXmlProperty(localName = "notes")
  public Notes notes;

  @JacksonXmlProperty(localName = "rrule")
  public Map<String, String> rrule;

  @JacksonXmlProperty(localName = "tasks")
  public List<Task> tasks = new ArrayList<>();

  public void setTask(Task singleTask) {
    this.tasks.add(singleTask);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("created", created)
        .add("modified", modified)
        .add("name", name)
        .add("source", source)
        .add("location_id", location_id)
        .add("url", url)
        .add("tags", tags)
        .add("participants", participants)
        .add("notes", notes)
        .add("rrule", rrule)
        .add("tasks", tasks)
        .toString();
  }
}
