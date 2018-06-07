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

public class Project {
  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("color")
  private int color;

  @JsonProperty("indent")
  private int indent;

  @JsonProperty("item_order")
  private int itemOrder;

  @JsonProperty("collapsed")
  private int collapsed;

  @JsonProperty("shared")
  private boolean shared;

  @JsonProperty("is_deleted")
  private int isDeleted;

  @JsonProperty("is_archived")
  private int isArchived;

  @JsonProperty("is_favorite")
  private int isFavorite;

  @JsonProperty("inbox_project")
  private boolean inboxProject;

  @JsonProperty("team_inbox")
  private boolean teamInbox;

}
