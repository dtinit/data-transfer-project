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
import com.google.common.base.MoreObjects;

public class SmugMugImage {
  @JsonProperty("Altitude")
  private long altitude;

  @JsonProperty("Caption")
  private String caption;

  @JsonProperty("Date")
  private String date;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("Format")
  private String format;

  @JsonProperty("Hidden")
  private boolean hidden;

  @JsonProperty("Keywords")
  private String keywords;

  @JsonProperty("Latitude")
  private double latitude;

  @JsonProperty("Longitude")
  private double longitude;

  @JsonProperty("Title")
  private String title;

  @JsonProperty("WebUri")
  private String webUri;

  public String getCaption() { return caption; }

  public String getFileName() { return fileName; }

  public String getFormat() { return format; }

  public String getTitle() { return title; }

  public String getWebUri() { return webUri; }

  public void setAltitude(long altitude) {
    this.altitude = altitude;
  }

  public void setCaption(String caption) { this.caption = caption; }

  public void setDate(String date) {
    this.date = date;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setWebUri(String webUri) {
    this.webUri = webUri;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("Title", title)
        .add("Caption", caption)
        .add("Date", date)
        .add("Hidden", hidden)
        .add("WebUri", webUri)
        .toString();
  }
}
