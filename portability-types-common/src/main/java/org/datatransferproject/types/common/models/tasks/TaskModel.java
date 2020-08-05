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
package org.datatransferproject.types.common.models.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class TaskModel {
  private final String taskListId;
  private final String text;
  private final String notes;
  private final Instant completedTime;  // null if incomplete
  private final Instant dueTime;  // null if not due

  @JsonCreator
  public TaskModel(
      @JsonProperty("taskListId") String taskListId,
      @JsonProperty("text") String text,
      @JsonProperty("notes") String notes,
      @JsonProperty("completedTime") Instant completedTime,
      @JsonProperty("dueTime") Instant dueTime) {
    this.taskListId = taskListId;
    this.text = text;
    this.notes = notes;
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

  public Instant getCompletedTime() { return completedTime; }

  public Instant getDueTime() { return dueTime; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskModel taskModel = (TaskModel) o;
    return Objects.equals(getTaskListId(), taskModel.getTaskListId()) &&
            Objects.equals(getText(), taskModel.getText()) &&
            Objects.equals(getNotes(), taskModel.getNotes()) &&
            Objects.equals(getCompletedTime(), taskModel.getCompletedTime()) &&
            Objects.equals(getDueTime(), taskModel.getDueTime());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTaskListId(), getText(), getNotes(), getCompletedTime(), getDueTime());
  }
}
