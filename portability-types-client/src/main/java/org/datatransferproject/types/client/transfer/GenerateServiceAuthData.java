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

/**
 * Request to generate transfer auth data for an export or import service.
 *
 * <p>This request is issued after the authorization token has been received for the export and
 * import services.
 */
public class GenerateServiceAuthData {
  public enum Mode {
    EXPORT,
    IMPORT
  }

  private final String id;
  private final String authToken;
  private final Mode mode;

  public GenerateServiceAuthData(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "authToken", required = true) String authToken,
      @JsonProperty(value = "mode", required = true) Mode mode) {
    this.id = id;
    this.authToken = authToken;
    this.mode = mode;
  }

  public String getId() {
    return id;
  }

  public String getAuthToken() {
    return authToken;
  }

  public Mode getMode() {
    return mode;
  }
}
