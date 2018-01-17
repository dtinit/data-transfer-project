/*
 * Copyright 2018 Google Inc.
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
package org.dataportabilityproject.serviceProviders.fiveHundredPx.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FiveHundredPxPhoto {
  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("WATERMARK")
  @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
  private boolean watermark;

  @JsonProperty("LICENSING_REQUESTED")
  @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
  private boolean licensing_requested;

  @JsonProperty("LICENSING_SUGGESTED")
  @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
  private boolean licensing_suggested;

  @JsonProperty("IS_FREE_PHOTO")
  @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
  private boolean is_free_photo;

  public int getId() { return id; }

  public String getName() { return name; }

  public String getDescription() { return description; }
}
