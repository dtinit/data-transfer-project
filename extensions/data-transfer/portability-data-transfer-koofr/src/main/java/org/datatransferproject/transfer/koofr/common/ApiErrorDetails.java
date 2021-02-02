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

public class ApiErrorDetails {
  private String code;
  private String message;

  public ApiErrorDetails(
      @JsonProperty("code") String code, @JsonProperty("message") String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "{" + " code='" + getCode() + "'" + ", message='" + getMessage() + "'" + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ApiErrorDetails)) {
      return false;
    }
    ApiErrorDetails apiErrorDetails = (ApiErrorDetails) o;
    return Objects.equals(code, apiErrorDetails.code)
        && Objects.equals(message, apiErrorDetails.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message);
  }
}
