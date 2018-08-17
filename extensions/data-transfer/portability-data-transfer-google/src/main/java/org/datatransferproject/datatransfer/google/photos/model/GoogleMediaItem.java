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

package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleMediaItem {
  @JsonProperty("id")
  private String id;

  @JsonProperty("description")
  private String description;

  @JsonProperty("productUrl")
  private String productUrl;

  @JsonProperty("baseUrl")
  private String baseUrl;

  @JsonProperty("mimeType")
  private String mimeType;

  @JsonProperty("mediaMetadata")
  private MediaMetadata mediaMetadata;

  @JsonProperty("contributorInfo")
  private ContributorInfo contributorInfo;

  @JsonProperty("location")
  private Object location; // Not yet available (2018-06-18)

  public String getId() { return id; }

  public String getDescription() { return description; }

  public String getProductUrl() { return productUrl; }

  public String getBaseUrl() { return baseUrl; }

  public String getMimeType() { return mimeType; }

  public MediaMetadata getMediaMetadata() { return mediaMetadata; }

  public ContributorInfo getContributorInfo() { return contributorInfo; }

  public void setId(String id) { this.id = id; }

  public void setDescription(String description) { this.description = description; }

  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

  public void setMimeType(String mimeType) { this.mimeType = mimeType; }

  public void setMediaMetadata(MediaMetadata mediaMetadata) { this.mediaMetadata = mediaMetadata; }
}
