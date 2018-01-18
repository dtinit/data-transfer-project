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

public class SmugMugAlbumImage {

  @JsonProperty("Title")
  private String title;

  @JsonProperty("Caption")
  private String caption;

  @JsonProperty("Keywords")
  private String keywords;

  @JsonProperty("Format")
  private String format;

  @JsonProperty("Latitude")
  private String latitude;

  @JsonProperty("Longitude")
  private String longitude;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("ArchivedUri")
  private String archivedUri;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public String getTitle() {
    return title;
  }

  public String getCaption() {
    return caption;
  }

  public String getFileName() {
    return fileName;
  }

  public String getArchivedUri() {
    return archivedUri;
  }

  public Map<String, SmugMugUrl> getUris() {
    return uris;
  }

  public String getFormat() {
    return format;
  }
}
