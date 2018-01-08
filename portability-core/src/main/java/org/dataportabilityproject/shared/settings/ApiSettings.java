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
package org.dataportabilityproject.shared.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Settings for {@code PortabilityApiServer}.
 */
public class ApiSettings {
  // TODO(rtannenbaum): Change these to URL types instead of String
  // Base url for all calls within the application
  private final String baseUrl;
  // Base url for direct to api calls within the application
  private final String baseApiUrl;

  @JsonCreator
  public ApiSettings(
      @JsonProperty(value="baseUrl", required=true) String baseUrl,
      @JsonProperty(value="baseApiUrl", required=true) String baseApiUrl) {
    this.baseUrl = baseUrl;
    this.baseApiUrl = baseApiUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getBaseApiUrl() {
    return baseApiUrl;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("baseUrl", baseUrl)
        .add("baseApiUrl", baseApiUrl)
        .toString();
  }
}
