/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models.social;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public class SocialActivityLocation {

  private final String name;
  private final double longitude;
  private final double latitude;

  @JsonCreator
  public SocialActivityLocation(
      @JsonProperty("name") String name,
      @JsonProperty("longitude") double longitude,
      @JsonProperty("latitude") double latitude) {

    this.name = name;
    this.longitude = longitude;
    this.latitude = latitude;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SocialActivityLocation that = (SocialActivityLocation) o;
    return Double.compare(that.longitude, longitude) == 0 &&
            Double.compare(that.latitude, latitude) == 0 &&
            Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, longitude, latitude);
  }

  public String getName() {
    return name;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }
}
