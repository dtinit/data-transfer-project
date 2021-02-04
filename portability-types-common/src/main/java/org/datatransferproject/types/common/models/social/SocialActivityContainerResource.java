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

package org.datatransferproject.types.common.models.social;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.datatransferproject.types.common.models.ContainerResource;

/**
 * Wrapper class for encoding social activity streams based on Activity Stream 2.0
 * (https://www.w3.org/TR/activitystreams-core/)
 *
 * <p>The wrapper is needed to allow DTP to page through large collections of items. DTP doesn't
 * know how to parse SocialActivityModel, only extensions that process social data will understand
 * this data, so it needs to by split up to allow DTP to efficiently process it.
 */
public class SocialActivityContainerResource extends ContainerResource {

  public static final String ACTIVITIES_COUNT_DATA_NAME = "activitiesCount";

  private final String id;
  private final Collection<SocialActivityModel> activities;
  private SocialActivityActor actor;

  @JsonCreator
  public SocialActivityContainerResource(
      @JsonProperty("id") String id,
      @JsonProperty("actor") SocialActivityActor actor,
      @JsonProperty("activities") Collection<SocialActivityModel> activities) {
    this.id = id;
    this.actor = actor;
    this.activities = activities == null ? ImmutableList.of() : activities;
  }

  public Collection<SocialActivityModel> getActivities() {
    return activities;
  }

  public SocialActivityActor getActor() {
    return actor;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SocialActivityContainerResource that = (SocialActivityContainerResource) o;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getActivities(), that.getActivities())
        && Objects.equals(getActor(), that.getActor());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getActivities(), getActor());
  }

  @Override
  public Map<String, Integer> getCounts() {
    return new ImmutableMap.Builder<String, Integer>()
        .put(ACTIVITIES_COUNT_DATA_NAME, activities.size())
        .build();
  }
}
