/*
 * Copyright 2017 Google Inc.
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
package org.dataportabilityproject.serviceProviders.instagram.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataModel for a image in the Instagram API, contains references to multiple resolutions.
 * Instantiated by JSON mapping.
 */
public final class ImageObject {

  private ImageData thumbnail;

  @JsonProperty("standard_resolution")
  private ImageData standard_resolution;

  @JsonProperty("low_resolution")
  private ImageData low_resolution;

  public ImageData getThumbnail() {
    return thumbnail;
  }

  public ImageData getStandardResolution() {
    return standard_resolution;
  }

  public ImageData getLowResolution() {
    return low_resolution;
  }
}
