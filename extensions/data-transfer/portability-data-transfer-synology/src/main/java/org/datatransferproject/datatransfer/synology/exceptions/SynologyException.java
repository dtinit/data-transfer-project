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

import javax.annotation.Nonnull;
import org.datatransferproject.datatransfer.synology.constant.SynologyErrorCode;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.FailureReasons;

/** Exception thrown when an error occurs during a Synology import / export operation. */
public class SynologyException extends CopyExceptionWithFailureReason {
  private final String errorCode;

  /**
   * @param message error message
   */
  protected SynologyException(String message) {
    super(message, new RuntimeException(message));
    this.errorCode = null;
  }

  /**
   * @param message error message
   * @param errorCode the error code
   */
  protected SynologyException(String message, String errorCode) {
    super(message, new RuntimeException(message));
    this.errorCode = errorCode;
  }

  /**
   * @param message error message
   * @param cause the cause of the exception
   * @param errorCode the error code
   */
  protected SynologyException(String message, Throwable cause, String errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    if (errorCode == null) {
      return FailureReasons.UPLOAD_ERROR.toString();
    }
    if (errorCode.equals(Integer.toString(SynologyErrorCode.QUOTA_NOT_CLAIMED))) {
      return FailureReasons.NO_NAS_IN_ACCOUNT.toString();
    }
    if (errorCode.equals(Integer.toString(SynologyErrorCode.DESTINATION_FULL))) {
      return FailureReasons.DESTINATION_FULL.toString();
    }
    return FailureReasons.UPLOAD_ERROR.toString();
  }
}
