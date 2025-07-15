/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.exceptions;

/**
 * Exception thrown by the Synology service. Message contains HTTP status code and response body.
 */
public class SynologyHttpException extends SynologyException {
  private final int statusCode;
  private final String responseBody;

  /**
   * @param message the message
   * @param statusCode the status code
   * @param responseBody the response body
   */
  public SynologyHttpException(String message, int statusCode, String responseBody) {
    super(message, "HTTP_" + statusCode);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  @Override
  public String getMessage() {
    return String.format(
        "%s (statusCode=%d, responseBody=%s)", super.getMessage(), statusCode, responseBody);
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getResponseBody() {
    return responseBody;
  }
}
