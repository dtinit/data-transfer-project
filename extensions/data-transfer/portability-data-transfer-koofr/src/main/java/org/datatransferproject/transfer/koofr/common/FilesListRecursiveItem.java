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
import java.util.Objects;

public class FilesListRecursiveItem {
  public static final String TYPE_FILE = "file";
  public static final String TYPE_ERROR = "error";

  private String type;
  private String path;
  private FilesFile file;
  private ApiErrorDetails error;

  public FilesListRecursiveItem(
      @JsonProperty("type") String type,
      @JsonProperty("path") String path,
      @JsonProperty("file") FilesFile file,
      @JsonProperty("error") ApiErrorDetails error) {
    this.type = type;
    this.path = path;
    this.file = file;
    this.error = error;
  }

  public String getType() {
    return type;
  }

  public String getPath() {
    return path;
  }

  public FilesFile getFile() {
    return file;
  }

  public ApiErrorDetails getError() {
    return error;
  }

  @Override
  public String toString() {
    return "{"
        + " type='"
        + getType()
        + "'"
        + ", path='"
        + getPath()
        + "'"
        + ", file='"
        + getFile()
        + "'"
        + ", error='"
        + getError()
        + "'"
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof FilesListRecursiveItem)) {
      return false;
    }
    FilesListRecursiveItem filesListRecursiveItem = (FilesListRecursiveItem) o;
    return Objects.equals(type, filesListRecursiveItem.type)
        && Objects.equals(path, filesListRecursiveItem.path)
        && Objects.equals(file, filesListRecursiveItem.file)
        && Objects.equals(error, filesListRecursiveItem.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, path, file, error);
  }
}
