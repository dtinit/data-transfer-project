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

package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaMetadata {
  @JsonProperty("creationTime")
  private String creationTime;

  @JsonProperty("width")
  private String width;

  @JsonProperty("height")
  private String height;

  @JsonProperty("photo")
  private Photo photo;

  @JsonProperty("video")
  private Video video;

  public String getCreationTime() {
    return creationTime;
  }

  public String getWidth() {
    return width;
  }

  public String getHeight() {
    return height;
  }

  public Photo getPhoto() {
    return photo;
  }

  public Video getVideo() {
    return video;
  }

  public void setPhoto(Photo photo) { this.photo = photo; }
}
