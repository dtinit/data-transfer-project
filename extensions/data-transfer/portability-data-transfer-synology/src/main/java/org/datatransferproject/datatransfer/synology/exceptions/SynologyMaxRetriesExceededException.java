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

import org.datatransferproject.datatransfer.synology.constant.SynologyErrorCode;

/**
 * Exception thrown when the maximum number of retries is exceeded.
 *
 * <p>Message includes the number of attempts and the cause of the exception.
 */
public class SynologyMaxRetriesExceededException extends SynologyDTPClientException {
  private final int attempts;

  public SynologyMaxRetriesExceededException(String message, int attempts, Throwable cause) {
    super(message, cause, String.valueOf(SynologyErrorCode.MAX_RETRIES_EXCEEDED));
    this.attempts = attempts;
  }

  @Override
  public String getMessage() {
    StringBuilder base = new StringBuilder(super.getMessage());
    base.append(" after ").append(attempts).append(" attempts");

    if (getCause() instanceof SynologyHttpException) {
      SynologyHttpException serviceEx = (SynologyHttpException) getCause();
      base.append(
          String.format(
              " (statusCode=%d, responseBody=%s)",
              serviceEx.getStatusCode(), serviceEx.getResponseBody()));
    }

    return base.toString();
  }

  public int getAttempts() {
    return attempts;
  }
}
