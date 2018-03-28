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

package org.dataportabilityproject.api.launcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/** Settings used in both the API and worker servers. */
public class CommonSettings {
  public enum Environment {
    LOCAL,
    TEST,
    QA,
    PROD
  }

  private final String cloud;
  private final Environment environment;

  @JsonCreator
  public CommonSettings(
      @JsonProperty(value = "cloud", required = true) String cloud,
      @JsonProperty(value = "environment", required = true) Environment environment) {
    this.cloud = cloud;
    this.environment = environment;
  }

  public String getCloud() {
    return cloud;
  }

  public Environment getEnvironment() {
    return environment;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("cloud", cloud)
        .add("environment", environment)
        .toString();
  }

  // prevent instantiation, use @JsonCreator constructor
  private CommonSettings() {
    cloud = null;
    environment = null;
  }
}
