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
import com.google.api.client.util.Key;

/** Represents a single Task */
public class Task {

  @JacksonXmlProperty(isAttribute = true, localName ="id")
  public int id;

  @JacksonXmlProperty(isAttribute = true, localName="due")
  public String due;

  @JacksonXmlProperty(isAttribute = true, localName="has_due_time")
  public int has_due_time;

  @JacksonXmlProperty(isAttribute = true, localName="added")
  public String added;

  @JacksonXmlProperty(isAttribute = true, localName="completed")
  public String completed;

  @JacksonXmlProperty(isAttribute = true, localName="deleted")
  public String deleted;

  @JacksonXmlProperty(isAttribute = true, localName="priority")
  public String priority;

  @JacksonXmlProperty(isAttribute = true, localName="postponed")
  public int postponed;

  @JacksonXmlProperty(isAttribute = true, localName="estimate")
  public String estimate;

  @Override
  public String toString() {
    return String.format(
        "Task(id=%d due=%s has_due_time=%s added=%s completed=%s deleted=%s priority=%s postponed=%s",
        id, due, has_due_time, added, completed, deleted, priority, postponed);
  }
}
