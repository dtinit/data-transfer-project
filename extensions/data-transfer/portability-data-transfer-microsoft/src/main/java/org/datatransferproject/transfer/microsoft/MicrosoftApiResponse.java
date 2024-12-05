/*
 * Copyright 2024 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.microsoft;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import java.io.IOException;
import java.util.Optional;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;

/** API response from Microsoft API servers. */
@AutoValue
public abstract class MicrosoftApiResponse {
  /** HTTP status code. */
  public abstract int httpStatus();

  /** HTTP status message. */
  public abstract String httpMessage();

  /** HTTP body of the response. */
  public abstract ResponseBody body();

  /** Builds from key fields within an HTTP response. */
  public static MicrosoftApiResponse ofResponse(Response response) {
    return MicrosoftApiResponse.builder()
        .setHttpStatus(response.code())
        .setBody(response.body())
        .setHttpMessage(response.message())
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHttpStatus(int httpStatus);

    public abstract Builder setHttpMessage(String httpMessage);

    public abstract Builder setBody(ResponseBody body);

    public abstract MicrosoftApiResponse build();
  }

  public static Builder builder() {
    return new org.datatransferproject.transfer.microsoft.AutoValue_MicrosoftApiResponse.Builder();
  }

  /**
   * Maps any error state of this response to a DTP-standard exception, if the response seems OK.
   *
   * <p>DTP exceptions are usually found in {@link org.datatransferproject.spi.transfer.types}.
   */
  public Optional<Exception> toDtpException() {
    if (isOkay()) {
      return Optional.empty();
    }

    if (isDestinationFull()) {
      return Optional.of(
          new DestinationMemoryFullException(
              "Microsoft destination storage limit reached", toIoException()));
    }

    /* DO NOT MERGE - collect all the cases. */

    return Optional.of(toIoException("unrecognized class of Microsoft API error"));
  }

  private boolean isOkay() {
    return httpStatus() >= 200 && httpStatus() <= 299;
  }

  /** Translate this response body to DTP's base-level exception. */
  private IOException toIoException() {
    checkState(!isOkay(), "isOkay(), so no exception to construct");
    return new IOException(toString());
  }

  /**
   * @param message The cause-message one might expect with a {@link java.lang.Exception}
   *     construction.
   */
  private IOException toIoException(String message) {
    checkState(!isOkay(), "isOkay(), so no exception to construct");
    return new IOException(String.format("%s: %s", message, toString()));
  }

  private boolean isDestinationFull() {
    return httpStatus() == 507 && httpMessage().contains("Insufficient Storage");
  }
}
