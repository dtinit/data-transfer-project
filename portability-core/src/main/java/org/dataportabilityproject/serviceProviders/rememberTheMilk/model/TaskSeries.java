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
package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Joiner;
import com.google.api.client.util.Key;
import java.util.List;

/**
 * A tasks series, see: https://www.rememberthemilk.com/services/api/tasks.rtm
 */
public class TaskSeries {

  @Key("@id")
  public int id;

  @Key("@created")
  public String created;

  @Key("@modified")
  public String modified;

  @Key("@name")
  public String name;

  @Key("@source")
  public String source;

  @Key("@authUrl")
  public String url;

  @Key("@location_id")
  public String location_id;

  @Key("tags")
  public String tags;

  @Key("participants")
  public String participants;

  @Key("notes")
  public Notes notes;

  @Key("task")
  public List<Task> tasks;

  @Override
  public String toString() {
    return String.format(
        "TaskSeries(id=%d created=%s modified=%s name=%s source=%s authUrl=%s, notes=%s tasks:%s)",
        id, created, modified, name, source, url, notes,
        (tasks == null || tasks.isEmpty()) ? "" : Joiner.on('\n').join(tasks));
  }
}
