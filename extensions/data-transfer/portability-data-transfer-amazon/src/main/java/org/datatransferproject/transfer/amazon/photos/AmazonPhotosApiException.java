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

package org.datatransferproject.transfer.amazon.photos;

import java.io.IOException;

/**
 * Typed exception for non-success Amazon Photos / Upload Service API responses.
 *
 * <p>Carries the HTTP status and the service {@code errorCode} (see {@link UploadErrorCodes})
 * so callers can classify failures on a structured field rather than by substring-matching a
 * raw response body. Extends {@link IOException} so it flows through the existing DTP
 * import error handling (e.g. {@code executeAndSwallowIOExceptions}).
 */
public class AmazonPhotosApiException extends IOException {

  private final int httpStatus;
  private final String errorCode;

  /**
   * @param httpStatus the HTTP status code of the response
   * @param errorCode the service errorCode from the response body, or {@code null} if absent
   * @param message a human-readable message (must not embed sensitive raw response detail)
   */
  public AmazonPhotosApiException(int httpStatus, String errorCode, String message) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  /** The service errorCode, or {@code null} if the response carried none. */
  public String getErrorCode() {
    return errorCode;
  }

  /** Whether this error's {@code errorCode} equals the given code. */
  public boolean isErrorCode(String code) {
    return code != null && code.equals(errorCode);
  }
}
