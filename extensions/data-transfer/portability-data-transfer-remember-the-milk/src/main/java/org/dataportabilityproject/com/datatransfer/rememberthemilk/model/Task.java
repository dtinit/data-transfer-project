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

import com.google.api.client.util.Key;

/**
 * Represents a single Task
 */
public class Task {

  @Key("@id")
  public int id;

  @Key("@due")
  public String due;

  @Key("@has_due_time")
  public boolean has_due_time;

  @Key("@added")
  public String added;

  @Key("@completed")
  public String completed;

  @Key("@deleted")
  public String deleted;

  @Key("@priority")
  public String priority;

  @Key("@postponed")
  public boolean postponed;

  @Key("@estimate")
  public String estimate;

  @Override
  public String toString() {
    return String.format(
        "Task(id=%d due=%s has_due_time=%s added=%s completed=%s deleted=%s priority=%s postponed=%s",
        id, due, has_due_time, added, completed, deleted, priority, postponed);
  }
}
