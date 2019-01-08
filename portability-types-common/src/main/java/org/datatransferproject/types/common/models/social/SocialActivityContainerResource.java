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
import com.ibm.common.activitystreams.Activity;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.datatransferproject.types.common.models.ContainerResource;

/**
 * Wrapper class for encoding social activity streams using Activity Stream 2.0
 * (https://www.w3.org/TR/activitystreams-core/)
 *
 * <p>The wrapper is needed to allow DTP to page through large collections of items.  DTP
 * doesn't know how to parse Activity Stream, only extensions that process social data will
 * understand this data, so it needs to by split up to allow DTP to efficiently process it.
 */
public class SocialActivityContainerResource extends ContainerResource {
  private final String id;
  private final Collection<Activity> activities;
  private final Collection<SocialActivityContainerResource> subContainers;

  @JsonCreator
  public SocialActivityContainerResource(
      @JsonProperty("id") String id,
      @JsonProperty("activities") Collection<Activity> activities,
      @JsonProperty("subContainers") Collection<SocialActivityContainerResource> subContainers) {
    this.id = id;
    this.activities = activities == null ? ImmutableList.of() : activities;
    this.subContainers = subContainers == null ? ImmutableList.of() : subContainers;
  }

  public Collection<Activity> getActivities() {
    return activities;
  }

  public Collection<SocialActivityContainerResource> getSubContainers() {
    return subContainers;
  }

  public String getId() {
    return id;
  }
}
