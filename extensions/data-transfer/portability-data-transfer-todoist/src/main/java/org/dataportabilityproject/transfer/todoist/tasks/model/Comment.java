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

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {
  @JsonProperty("id")
  private int id;

  @JsonProperty("task_id")
  private int taskId;

  @JsonProperty("project_id")
  private int projectId;

  @JsonProperty("posted")
  private String posted;

  @JsonProperty("content")
  private String content;

  @JsonProperty("attachment")
  private Object attachment;
}
