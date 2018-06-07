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

// TODO: Notes are only available for premium users, how to handle this?
public class Note {
  @JsonProperty("id")
  private int id;

  @JsonProperty("posted_uid")
  private int postedUid;

  @JsonProperty("item_id")
  private int itemId;

  @JsonProperty("project_id")
  private int projectId;

  @JsonProperty("content")
  private String content;

  @JsonProperty("file_attachment")
  private Object fileAttachment;

  @JsonProperty("uids_to_notify")
  private int[] uidsToNotify;

  @JsonProperty("is_deleted")
  private int isDeleted;

  @JsonProperty("is_archived")
  private int isArchived;

  @JsonProperty("posted")
  private String posted;
}
