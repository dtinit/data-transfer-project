/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.audiomack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A generic wrapper around Audiomack responses. */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AudiomackResponse<T> {

  @JsonProperty("results")
  private T results;

  @JsonProperty("paging_token")
  private String paginationToken;

  @JsonProperty("errorcode")
  private int errorCode;

  @JsonProperty("message")
  private String errorMessage;

  public T getResults() { return results; }

  public String getPaginationToken() { return paginationToken; }

  public int getErrorCode() { return errorCode; }

  public String getErrorMessage() { return errorMessage; }
}
