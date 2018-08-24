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
package org.datatransferproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request to start a transfer job. */
public class StartTransferJob {
  private final String id;
  private final String encryptedAuthData;

  public StartTransferJob(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "encryptedAuthData", required = true) String encryptedAuthData) {
    this.id = id;
    this.encryptedAuthData = encryptedAuthData;
  }

  public String getId() {
    return id;
  }

  public String getEncryptedAuthData() {
    return encryptedAuthData;
  }
}
