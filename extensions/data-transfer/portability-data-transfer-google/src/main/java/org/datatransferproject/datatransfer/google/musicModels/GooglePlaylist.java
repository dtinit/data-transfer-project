/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class representing a playlist as returned by the Google Music API.
 */
public class GooglePlaylist {

  @JsonProperty("name")
  private String name;

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  // In JSON format, the Timestamp type is encoded as a string in the
  // [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format. That is, the
  // format is "{year}-{month}-{day}T{hour}:{min}:{sec}[.{frac_sec}]Z".
  @JsonProperty("createTime")
  private String createTime;

  @JsonProperty("updateTime")
  private String updateTime;

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getCreateTime() {
    return createTime;
  }

  public String getUpdateTime() {
    return updateTime;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCreateTime(String createTime) {
    this.createTime = createTime;
  }

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }
}
