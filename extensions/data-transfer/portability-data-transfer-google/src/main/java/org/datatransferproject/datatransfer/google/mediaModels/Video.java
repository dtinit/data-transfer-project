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
 * Video metadata - not the content of the video itself!
 */
public class Video {
  @JsonProperty("cameraMake")
  private String cameraMake;

  @JsonProperty("cameraModel")
  private String cameraModel;

  @JsonProperty("fps")
  private double fps;

  @JsonProperty("status")
  private VideoProcessingStatus status;

  public enum VideoProcessingStatus {
    UNSPECIFIED,
    PROCESSING,
    READY,
    FAILED;
  }
}
