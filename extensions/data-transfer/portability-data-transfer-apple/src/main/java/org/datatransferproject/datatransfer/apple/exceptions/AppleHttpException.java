/*
 * Copyright 2023 The Data Transfer Project Authors.
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

import javax.annotation.Nonnull;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.jetbrains.annotations.NotNull;

/**
 * A generic Exception for all Apple Transfer HTTP APIs.
 */
public class AppleHttpException extends CopyExceptionWithFailureReason {

    private final int responseStatus;

    public AppleHttpException(@NotNull final String message, @NotNull final Throwable cause, @NotNull final int responseStatus) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    @Nonnull
    @Override
    public String getFailureReason() {
        return getMessage();
    }
}
