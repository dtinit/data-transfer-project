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

package org.dataportabilityproject.transfer.todoist.tasks.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class Task {

  private String id;
  private String projectId;
  private String content;
  private boolean completed;
  private String[] labelIds;
  private int order;
  private int indent;
  private int priority;
  private Due due;
  private String url;
  private int commentCount;

  @JsonCreator
  public Task(
      @JsonProperty("id") String id,
      @JsonProperty("project_id") String projectId,
      @JsonProperty("content") String content,
      @JsonProperty("completed") Boolean completed,
      @JsonProperty("label_ids") String[] labelIds,
      @JsonProperty("order") Integer order,
      @JsonProperty("indent") Integer indent,
      @JsonProperty("priority") Integer priority,
      @JsonProperty("due") Due due,
      @JsonProperty("url") String url,
      @JsonProperty("comment_count") Integer commentCount
  ) {
    this.id = id;
    this.projectId = projectId;
    this.content = content;
    this.completed = completed;
    this.labelIds = labelIds;
    this.order = order;
    this.indent = indent;
    this.priority = priority;
    this.due = due;
    this.url = url;
    this.commentCount = commentCount;
  }

  public String getId() {
    return id;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getContent() {
    return content;
  }

  public boolean getCompleted() {
    return completed;
  }

  public Due getDue() {
    return due;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("project_id", projectId)
        .add("content", content)
        .add("completed", completed)
        .add("label_ids", labelIds)
        .add("order", order)
        .add("indent", indent)
        .add("priority", priority)
        .add("due", due)
        .add("url", url)
        .add("comment_count", commentCount)
        .toString();
  }
}
