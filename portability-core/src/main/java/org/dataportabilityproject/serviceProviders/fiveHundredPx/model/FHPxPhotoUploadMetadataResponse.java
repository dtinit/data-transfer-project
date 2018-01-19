/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.fiveHundredPx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FHPxPhotoUploadMetadataResponse {
  @JsonProperty("upload_key")
  private String uploadKey;

  @JsonProperty("keyword_key")
  private String keywordKey;

  @JsonProperty("presigned_post")
  private FHPxPresignedPost presignedPost;

  @JsonProperty("photo")
  private FHPxPhoto photo;

  public String getUploadKey() { return uploadKey; }

  public String getKeywordKey() { return keywordKey; }

  public FHPxPresignedPost getPresignedPost() { return presignedPost; }

  public FHPxPhoto getPhoto() { return photo; }

  class FHPxPresignedPost {
    @JsonProperty("url")
    private String url;

    @JsonProperty("fields")
    private Map<String, String> fields;

    public String getUrl() { return url; }

    public Map<String, String> getFields() { return fields; }
  }
}
