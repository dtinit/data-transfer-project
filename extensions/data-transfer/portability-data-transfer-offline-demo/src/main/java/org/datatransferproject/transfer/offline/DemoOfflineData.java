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
package org.datatransferproject.transfer.offline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.DataModel;

/**
 * Encapsulates offline data for the demo extension. Note the format of the contents is opaque; they
 * may change without notice.
 */
@JsonTypeName("org.dataportability:DemoOfflineData")
public class DemoOfflineData extends DataModel {

  private final String contents;

  @JsonCreator
  public DemoOfflineData(@JsonProperty("contents") String contents) {
    this.contents = contents;
  }

  public String getContents() {
    return contents;
  }
}
