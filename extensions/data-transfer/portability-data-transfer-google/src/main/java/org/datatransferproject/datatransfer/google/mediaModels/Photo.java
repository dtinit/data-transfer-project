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
 * Photo metadata - not the content of the photo itself!
 */
public class Photo {
  @JsonProperty("cameraMake")
  private String cameraMake;

  @JsonProperty("cameraModel")
  private String cameraModel;

  @JsonProperty("focalLength")
  private double focalLength;

  @JsonProperty("apertureFNumber")
  private double apertureFNumber;

  @JsonProperty("isoEquivalent")
  private double isoEquivalent;

  @JsonProperty("exposureTime")
  private String exposureTime;
}
