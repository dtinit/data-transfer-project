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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("org.dataportability:SmugMugAlbum")
public class SmugMugAlbum {
  private final String date;
  private final String description;
  private final String name;
  private final String privacy;
  private final String uri;
  private final String urlName;
  private final String webUri;

  public SmugMugAlbum(
    @JsonProperty("Date") String date, 
    @JsonProperty("Description") String description, 
    @JsonProperty("Name") String name, 
    @JsonProperty("Privacy") String privacy, 
    @JsonProperty("Uri") String uri, 
    @JsonProperty("UrlName") String urlName, 
    @JsonProperty("WebUri") String webUri) {
    this.date = date;
    this.description = description;
    this.name = name;
    this.privacy = privacy;
    this.uri = uri;
    this.urlName = urlName;
    this.webUri = webUri;
  }

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
