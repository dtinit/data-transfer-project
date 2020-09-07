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

import java.util.Objects;

public class SocialActivityAttachment {
  private SocialActivityAttachmentType type;
  private String url;
  private String name;
  private String content;

  @JsonCreator
  public SocialActivityAttachment(
      @JsonProperty("type") SocialActivityAttachmentType type,
      @JsonProperty("url") String url,
      @JsonProperty("name") String name,
      @JsonProperty("content") String content) {
    this.type = type;
    this.url = url;
    this.name = name;
    this.content = content;
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }

  public String getContent() {
    return content;
  }

  public SocialActivityAttachmentType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SocialActivityAttachment that = (SocialActivityAttachment) o;
    return getType() == that.getType()
        && Objects.equals(getUrl(), that.getUrl())
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getContent(), that.getContent());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getUrl(), getName(), getContent());
  }
}
