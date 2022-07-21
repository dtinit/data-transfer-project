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

package org.datatransferproject.transfer.microsoft.driveModels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Microsoft video metadata resource type Ref:
 * https://docs.microsoft.com/en-us/graph/api/resources/video?view=graph-rest-1.0
 */
public class MicrosoftVideoMetadata {
  /** Number of audio bits per sample. */
  @JsonProperty("audioBitsPerSample")
  public int audioBitsPerSample;

  /** Number of audio channels. */
  @JsonProperty("audioChannels")
  public int audioChannels;

  /** Name of the audio format (AAC, MP3, etc.). */
  @JsonProperty("audioFormat")
  public String audioFormat;

  /** Number of audio samples per second. */
  @JsonProperty("audioSamplesPerSecond")
  public int audioSamplesPerSecond;

  /** Bit rate of the video in bits per second. */
  @JsonProperty("bitrate")
  public int bitrate;

  /** Duration of the file in milliseconds. */
  @JsonProperty("duration")
  public long duration;

  /** "Four character code" name of the video format. */
  @JsonProperty("fourCC")
  public String fourCC;

  /** Frame rate of the video. */
  @JsonProperty("frameRate")
  public double frameRate;

  /** Height of the video, in pixels. */
  @JsonProperty("height")
  public int height;

  /** Width of the video, in pixels. */
  @JsonProperty("width")
  public int width;
}
