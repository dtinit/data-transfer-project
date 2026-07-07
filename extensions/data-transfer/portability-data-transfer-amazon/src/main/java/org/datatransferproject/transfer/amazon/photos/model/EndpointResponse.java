/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response model for GET /account/endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointResponse {

  @JsonProperty("metadataUrl")
  private String metadataUrl;

  @JsonProperty("contentUrl")
  private String contentUrl;

  @JsonProperty("uploadServiceUrl")
  private String uploadServiceUrl;

  public String getMetadataUrl() { return metadataUrl; }
  public String getContentUrl() { return contentUrl; }
  public String getUploadServiceUrl() { return uploadServiceUrl; }
}
