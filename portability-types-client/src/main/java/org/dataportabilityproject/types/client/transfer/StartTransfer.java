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
package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request to initiate the data transfer process */
public class StartTransfer {
  private String id;
  private String exportAuthData;
  private String importAuthData;

  public StartTransfer(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "exportAuthData", required = true) String exportAuthData,
      @JsonProperty(value = "importAuthData", required = true) String importAuthData) {
    this.id = id;
    this.exportAuthData = exportAuthData;
    this.importAuthData = importAuthData;
  }

  public String getId() {
    return id;
  }

  /** Returns encrypted auth data for the export service. */
  public String getExportAuthData() {
    return exportAuthData;
  }

  /** Returns encrypted auth data for the import service. */
  public String getImportAuthData() {
    return importAuthData;
  }
}
