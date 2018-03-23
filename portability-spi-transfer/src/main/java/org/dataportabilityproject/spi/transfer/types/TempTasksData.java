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

package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.types.transfer.models.DataModel;

@JsonTypeName("org.dataportability:TempTasksData")
public class TempTasksData extends DataModel {

  @JsonProperty("jobId")
  private final String jobId;

  @JsonProperty("newTaskListIds")
  private final Map<String, String> newTaskListIds;

  public TempTasksData(@JsonProperty("jobId") String jobId) {
    this.jobId = jobId;
    this.newTaskListIds = new HashMap<>();
  }

  public void addTaskListId(String oldTaskListId, String newTaskListId) {
    newTaskListIds.put(oldTaskListId, newTaskListId);
  }

  public String lookupNewTaskListId(String oldTaskListId) {
    return newTaskListIds.getOrDefault(oldTaskListId, "");
  }


}
