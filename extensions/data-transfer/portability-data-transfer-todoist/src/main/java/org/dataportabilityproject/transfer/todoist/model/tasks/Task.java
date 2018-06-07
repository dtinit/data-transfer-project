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

package org.dataportabilityproject.transfer.todoist.model.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Task {
  @JsonProperty("id")
  private int id;

  @JsonProperty("project_id")
  private int projectId;

  @JsonProperty("content")
  private String content;

  @JsonProperty("completed")
  private boolean completed;

  @JsonProperty("label_ids")
  private int[] labelIds;

  @JsonProperty("order")
  private int order;

  @JsonProperty("indent")
  private int indent;

  @JsonProperty("priority")
  private int priority;

  @JsonProperty("due")
  private Due due;

  @JsonProperty("url")
  private String url;

  @JsonProperty("comment_count")
  private int commentCount;
}
