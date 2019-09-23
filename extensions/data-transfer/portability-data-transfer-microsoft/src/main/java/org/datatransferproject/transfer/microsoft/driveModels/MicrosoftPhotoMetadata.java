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

package org.datatransferproject.transfer.microsoft.driveModels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Microsoft photo metadata resource type Ref:
 * https://docs.microsoft.com/en-us/graph/api/resources/photo?view=graph-rest-1.0
 */
public class MicrosoftPhotoMetadata {

  @JsonProperty("cameraMake")
  public String cameraMake;

  @JsonProperty("cameraModel")
  public String cameraModel;

  @JsonProperty("fNumber")
  public double fNumber;

  @JsonProperty("exposureDenominator")
  public double exposureDenominator;

  @JsonProperty("exposureNumerator")
  public double exposureNumerator;

  @JsonProperty("focalLength")
  public double focalLength;

  @JsonProperty("iso")
  public int iso;
}
