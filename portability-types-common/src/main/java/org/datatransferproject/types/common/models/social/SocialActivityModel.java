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
import com.google.common.collect.ImmutableList;
import org.datatransferproject.types.common.ImportableItem;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public class SocialActivityModel implements ImportableItem {
  private final String id;
  private final Instant published;
  private final SocialActivityType type;
  private final Collection<SocialActivityAttachment> attachments;
  private final SocialActivityLocation location;
  private final String title;
  private final String content;
  private final String url;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SocialActivityModel that = (SocialActivityModel) o;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getPublished(), that.getPublished())
        && getType() == that.getType()
        && Objects.equals(getAttachments(), that.getAttachments())
        && Objects.equals(getLocation(), that.getLocation())
        && Objects.equals(getTitle(), that.getTitle())
        && Objects.equals(getContent(), that.getContent())
        && Objects.equals(getUrl(), that.getUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getPublished(),
        getType(),
        getAttachments(),
        getLocation(),
        getTitle(),
        getContent(),
        getUrl());
  }

  @JsonCreator
  public SocialActivityModel(
      @JsonProperty("id") String id,
      @JsonProperty("published") Instant published,
      @JsonProperty("type") SocialActivityType type,
      @JsonProperty("attachments") Collection<SocialActivityAttachment> attachments,
      @JsonProperty("location") SocialActivityLocation location,
      @JsonProperty("title") String title,
      @JsonProperty("content") String content,
      @JsonProperty("url") String url) {
    this.id = id;
    this.published = published;
    this.type = type;
    this.attachments = attachments == null ? ImmutableList.of() : attachments;
    this.location = location;
    this.title = title;
    this.content = content;
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public Instant getPublished() {
    return published;
  }

  public SocialActivityType getType() {
    return type;
  }

  public Collection<SocialActivityAttachment> getAttachments() {
    return attachments;
  }

  public SocialActivityLocation getLocation() {
    return location;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getUrl() {
    return url;
  }

  @Nonnull
  @Override
  public String getIdempotentId() {
    return getId();
  }

  @Override
  public String getName() { return title; }
}
