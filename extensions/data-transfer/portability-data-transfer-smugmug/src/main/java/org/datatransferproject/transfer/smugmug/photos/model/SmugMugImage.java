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

import java.util.Map;

public class SmugMugImage {
  @JsonProperty("Title")
  private String title;

  @JsonProperty("Caption")
  private String caption;

  @JsonProperty("Hidden")
  private boolean hidden;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("Format")
  private String format;

  @JsonProperty("UploadKey")
  private String uploadKey;

  @JsonProperty("ArchivedUri")
  private String archivedUri;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public void setTitle(String title) {
    this.title = title;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getTitle() {
    return title;
  }

  public String getCaption() {
    return caption;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFormat() {
    return format;
  }

  public String getArchivedUri() {
    return archivedUri;
  }
}
