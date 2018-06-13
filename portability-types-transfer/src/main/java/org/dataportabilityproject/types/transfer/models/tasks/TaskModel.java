/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.dataportabilityproject.types.transfer.models.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class TaskModel {
  private final String taskListId;
  private final String text;
  private final String notes;
  private final boolean completed;
  private final Instant completedTime;  // null if incomplete or not provided
  private final Instant dueTime;  // null if not due

  @JsonCreator
  public TaskModel(
      @JsonProperty("taskListId") String taskListId,
      @JsonProperty("text") String text,
      @JsonProperty("notes") String notes,
      @JsonProperty("completed") boolean completed,
      @JsonProperty("completedTime") Instant completedTime,
      @JsonProperty("dueTime") Instant dueTime) {
    this.taskListId = taskListId;
    this.text = text;
    this.notes = notes;
    this.completed = completed;
    this.completedTime = completedTime;
    this.dueTime = dueTime;
  }

  public String getText() {
    return text;
  }

  public String getNotes() {
    return notes;
  }

  public String getTaskListId() {
    return taskListId;
  }

  public boolean isCompleted() { return completed; }

  public Instant getCompletedTime() { return completedTime; }

  public Instant getDueTime() { return dueTime; }
}
