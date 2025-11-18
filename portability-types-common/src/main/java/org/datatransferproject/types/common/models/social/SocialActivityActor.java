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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.datatransferproject.types.common.ImportableItem;

import javax.annotation.Nonnull;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public class SocialActivityActor implements ImportableItem {
  private final String url;
  private final String name;
  private final String id;

  @JsonCreator
  public SocialActivityActor(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("url") String url) {

    this.id = id;
    this.name = name;
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @JsonIgnore(false)
  @Override
  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  @Nonnull
  @Override
  public String getIdempotentId() {
    return getId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SocialActivityActor actor = (SocialActivityActor) o;
    return Objects.equals(getUrl(), actor.getUrl()) &&
            Objects.equals(getName(), actor.getName()) &&
            Objects.equals(getId(), actor.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUrl(), getName(), getId());
  }
}
