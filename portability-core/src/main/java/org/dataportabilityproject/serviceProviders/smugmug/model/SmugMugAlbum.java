/*
 * Copyright 2017 Google Inc.
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
package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SmugMugAlbum {

  @JsonProperty("NiceName")
  private String niceName;

  @JsonProperty("UrlName")
  private String urlName;

  @JsonProperty("Title")
  private String title;

  @JsonProperty("Name")
  private String name;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("AllowDownloads")
  private String allowDownloads;

  @JsonProperty("AlbumKey")
  private String albumKey;

  @JsonProperty("NodeID")
  private String nodeID;

  @JsonProperty("ImageCount")
  private int imageCount;

  @JsonProperty("UrlPath")
  private String urlPath;

  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public Map<String, SmugMugUrl> getUris() {
    return uris;
  }

  public String getTitle() {
    return title;
  }

  public String getAlbumKey() {
    return albumKey;
  }

  public String getDescription() {
    return description;
  }
}
