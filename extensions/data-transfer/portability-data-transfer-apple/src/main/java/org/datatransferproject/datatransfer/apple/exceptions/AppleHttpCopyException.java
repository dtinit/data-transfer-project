/*
 * Copyright 2024 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.exceptions;

import org.datatransferproject.spi.transfer.types.CopyException;

import javax.annotation.Nonnull;

/**
 * A generic Exception for all Apple Transfer HTTP APIs.
 */
public class AppleHttpCopyException extends CopyException {
  private final int responseStatus;

  public AppleHttpCopyException(@Nonnull final String message, @Nonnull final Throwable cause, final int responseStatus) {
    super(message, cause);
    this.responseStatus = responseStatus;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  @Nonnull
  public String getFailureReason() {
    return getMessage();
  }
}
