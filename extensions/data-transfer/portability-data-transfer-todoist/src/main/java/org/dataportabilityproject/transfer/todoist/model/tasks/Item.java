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

public class Item {

  @JsonProperty("id")
  private int id;

  @JsonProperty("user_id")
  private int userId;

  @JsonProperty("project_id")
  private int projectId;

  @JsonProperty("content")
  private String content;

  @JsonProperty("date_string")
  private String dateString;

  @JsonProperty("date_lang")
  private String dateLang;

  @JsonProperty("due_date_utc")
  private String dueDateUtc;

  @JsonProperty("priority")
  private int priority;

  @JsonProperty("indent")
  private int indent;

  @JsonProperty("item_order")
  private int itemOrder;

  @JsonProperty("day_order")
  private int dayOrder;

  @JsonProperty("collapsed")
  private int collapsed;

  @JsonProperty("labels")
  private int[] labels;

  @JsonProperty("assigned_by_uid")
  private int assignedByUid;

  @JsonProperty("responsible_uid")
  private int responsibleUid;

  @JsonProperty("checked")
  private int checked;

  @JsonProperty("in_history")
  private int inHistory;

  @JsonProperty("is_deleted")
  private int isDeleted;

  @JsonProperty("is_archived")
  private int isArchived;

  @JsonProperty("sync_id")
  private int syncId;

  @JsonProperty("date_added")
  private String dateAdded;
}
