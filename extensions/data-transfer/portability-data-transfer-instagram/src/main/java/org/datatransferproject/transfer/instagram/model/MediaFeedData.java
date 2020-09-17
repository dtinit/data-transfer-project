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

package org.datatransferproject.transfer.instagram.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

/** DataModel for a media feed in the Instagram API. Instantiated by JSON mapping. */
public final class MediaFeedData {

  @JsonProperty("id")
  private String id;

  @JsonProperty("media_type")
  private String media_type;

  @JsonProperty("media_url")
  private String media_url;

  @JsonProperty("caption")
  private String caption;

  @JsonProperty("timestamp")
  private Date publish_date;

  public String getId() {
    return id;
  }

  public String getMediaType() {
    return media_type;
  }

  public String getMediaUrl() {
    return media_url;
  }

  public String getCaption() {
    return caption;
  }

  public Date getPublishDate() {
    return publish_date;
  }
}
