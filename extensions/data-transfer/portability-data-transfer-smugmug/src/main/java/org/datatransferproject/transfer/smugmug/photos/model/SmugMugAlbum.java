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
package org.datatransferproject.transfer.smugmug.photos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class SmugMugAlbum implements Serializable {

  @JsonProperty("Date")
  private String date;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("Name")
  private String name;

  @JsonProperty("Privacy")
  private String privacy;

  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("UrlName")
  private String urlName;

  @JsonProperty("WebUri")
  private String webUri;

  public String getDate() {
    return date;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public String getPrivacy() {
    return privacy;
  }

  public String getUri() {
    return uri;
  }

  public String getUrlName() {
    return urlName;
  }

  public String getWebUri() {
    return webUri;
  }
}
