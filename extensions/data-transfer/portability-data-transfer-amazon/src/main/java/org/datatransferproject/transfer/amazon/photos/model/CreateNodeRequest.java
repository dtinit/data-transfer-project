/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for node creation API (POST /nodes) and upload metadata. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateNodeRequest {

  @JsonProperty("name")
  private final String name;

  @JsonProperty("kind")
  private final String kind;

  @JsonProperty("conflictResolution")
  private final String conflictResolution;

  // Omitted from the JSON entirely when null or empty, rather than emitting "[]".
  @JsonProperty("labels")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final List<String> labels;

  public CreateNodeRequest(String name, String kind) {
    this(name, kind, null, null);
  }

  public CreateNodeRequest(String name, String kind, String conflictResolution,
                           List<String> labels) {
    this.name = name;
    this.kind = kind;
    this.conflictResolution = conflictResolution;
    this.labels = labels;
  }
}
