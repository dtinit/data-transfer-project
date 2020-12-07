/*
 * Copyright 2020 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.koofr.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FilesFile {
  public static final String TYPE_FILE = "file";
  public static final String TYPE_DIR = "dir";

  private String name;
  private String type;
  private Long modified;
  private Long size;
  private String contentType;
  private String hash;
  private Map<String, List<String>> tags;

  public FilesFile(
      @JsonProperty("name") String name,
      @JsonProperty("type") String type,
      @JsonProperty("modified") Long modified,
      @JsonProperty("size") Long size,
      @JsonProperty("contentType") String contentType,
      @JsonProperty("hash") String hash,
      @JsonProperty("tags") Map<String, List<String>> tags) {
    this.name = name;
    this.type = type;
    this.modified = modified;
    this.size = size;
    this.contentType = contentType;
    this.hash = hash;
    this.tags = tags;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Long getModified() {
    return modified;
  }

  public Long getSize() {
    return size;
  }

  public String getContentType() {
    return contentType;
  }

  public String getHash() {
    return hash;
  }

  public Map<String, List<String>> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return "{"
        + " name='"
        + getName()
        + "'"
        + ", type='"
        + getType()
        + "'"
        + ", modified='"
        + getModified()
        + "'"
        + ", size='"
        + getSize()
        + "'"
        + ", contentType='"
        + getContentType()
        + "'"
        + ", hash='"
        + getHash()
        + "'"
        + ", tags='"
        + getTags()
        + "'"
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof FilesFile)) {
      return false;
    }
    FilesFile filesFile = (FilesFile) o;
    return Objects.equals(name, filesFile.name)
        && Objects.equals(type, filesFile.type)
        && Objects.equals(modified, filesFile.modified)
        && Objects.equals(size, filesFile.size)
        && Objects.equals(contentType, filesFile.contentType)
        && Objects.equals(hash, filesFile.hash)
        && Objects.equals(tags, filesFile.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, modified, size, contentType, hash, tags);
  }
}
