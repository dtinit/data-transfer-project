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

package org.datatransferproject.datatransfer.backblaze.exception;

import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;

public final class BackblazeCredentialsException extends CopyExceptionWithFailureReason {
  public BackblazeCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getFailureReason() {
    return "INVALID_MANUAL_CREDENTIALS";
  }
}
