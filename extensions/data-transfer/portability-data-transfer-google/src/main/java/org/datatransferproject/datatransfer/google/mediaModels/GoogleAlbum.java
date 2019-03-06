/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.mediaModels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class representing an album as returned by the Google Photos API.
 */
public class GoogleAlbum {
  @JsonProperty("id")
  private String id;

  @JsonProperty("title")
  private String title;

  public String getId() { return id; }

  public String getTitle() { return title; }

  public void setId(String id) { this.id = id; }

  public void setTitle(String title) { this.title = title; }
}
