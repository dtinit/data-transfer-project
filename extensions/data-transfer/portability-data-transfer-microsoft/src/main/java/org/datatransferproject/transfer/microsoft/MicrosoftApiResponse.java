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
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;

/**
 * API response from Microsoft API servers.
 *
 * <p>Example usage error handling workflow:
 *
 * <pre>
 * MicrosoftApiResponse resp = someServerCall();
 * Optional<RecoverableState> recovery = resp.recoverableState();
 * if (recovery.isPresent()) {
 *   switch (recovery.get()) {
 *     case RECOVERABLE_STATE_OKAY:
 *       return; // no errors to handle! we're all set
 *     case RECOVERABLE_STATE_NEEDS_TOKEN_REFRESH:
 *        // ... run a token-refresh and trigger your retry control-flow
 *     default:
 *       throw new AssertionError("exhaustive switch");
 *   }
 * } else {
 *   resp.throwDtpException();
 * }
 * </pre>
 */
@AutoValue
public abstract class MicrosoftApiResponse {
  /** HTTP status code. */
  public abstract int httpStatus();

  /** HTTP status message. */
  public abstract String httpMessage();

  /** HTTP body of the response if any was present. */
  public abstract Optional<ResponseBody> body();

  /** Builds from key fields within an HTTP response. */
  public static MicrosoftApiResponse ofResponse(Response response) {
    Builder builder =
        MicrosoftApiResponse.builder()
            .setHttpMessage(response.message())
            .setHttpStatus(response.code());

    if (response.body() != null) {
      builder.setBody(response.body());
    }

    return builder.build();
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

  public enum RecoverableState {
    RECOVERABLE_STATE_OKAY,
    RECOVERABLE_STATE_NEEDS_TOKEN_REFRESH,
  }

  public enum FatalState {
    FATAL_STATE_FATAL_PERMISSION_DENIED,
    FATAL_STATE_FATAL_DESTINATION_FULL,
    FATAL_STATE_FATAL_UNSPECIFIED
  }

  /**
   * Returns the recoverable state of this response, if there is one, otherwise call {@link
   * toFatalState} or {@link throwDtpException} should detail what's wrong.
   */
  public Optional<RecoverableState> recoverableState() {
    if (isOkay()) {
      return Optional.of(RecoverableState.RECOVERABLE_STATE_OKAY);
    } else if (isTokenRefreshRequired()) {
      return Optional.of(RecoverableState.RECOVERABLE_STATE_NEEDS_TOKEN_REFRESH);
    } else {
      return Optional.empty(); // unrecoverable state
    }
  }

  /** Whether response indicates you might be able to keep going. */
  private boolean hasRecovery() {
    return recoverableState().isPresent();
  }

  /** Whether the response was OK, otherwise {@link throwDtpException} should return something. */
  public boolean isOkay() {
    return httpStatus() >= 200 && httpStatus() <= 299;
  }

  /**
   * Whether the response health is neither OK nor outright error, but merely requires a token
   * refresh (then a retry).
   */
  private boolean isTokenRefreshRequired() {
    return httpStatus() == 401;
  }

  /** Whether response is of an unrecoverable error, indicating {@link throwDtpException} usage. */
  private boolean isFatal() {
    return !hasRecovery();
  }

  /**
   * Maps current error state of this response to a DTP-standard exception, if the response seems OK
   * (note: this is nonsensical to call when not {@link isFatal}).
   *
   * <p>DTP exceptions are usually found in {@link org.datatransferproject.spi.transfer.types}. The
   * base "cause" of exceptions constructed here will always at least be a read-out of the API
   * response itself, and possible some more explanatory text.
   *
   * <p>Throws IllegalStateException if {@link isFatal} is false.
   */
  public void throwDtpException()
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    switch (toFatalState()) {
      case FATAL_STATE_FATAL_PERMISSION_DENIED:
        throw new PermissionDeniedException(
            "User access to microsoft onedrive was denied", toIoException());
      case FATAL_STATE_FATAL_DESTINATION_FULL:
        throw new DestinationMemoryFullException(
            "Microsoft destination storage limit reached", toIoException());
      case FATAL_STATE_FATAL_UNSPECIFIED:
        throw toIoException("unrecognized class of Microsoft API error");
      default:
        throw new AssertionError("exhaustive switch");
    }
  }

  private FatalState toFatalState() {
    checkState(isFatal(), "cannot explain fatal state when is apparently recoverable");
    if (httpStatus() == 403 && httpMessage().contains("Access Denied")) {
      return FatalState.FATAL_STATE_FATAL_PERMISSION_DENIED;
    }
    // TODO(zacsh) fix destination full check, it's currently the wrong string.
    if (httpStatus() == 507 && httpMessage().contains("Insufficient Storage")) {
      return FatalState.FATAL_STATE_FATAL_DESTINATION_FULL;
    }
    return FatalState.FATAL_STATE_FATAL_UNSPECIFIED;
  }

  /**
   * Translate this response body to DTP's base-level exception with a cause message that includes
   * all the details we have.
   */
  public IOException toIoException() {
    return new IOException(toString()); // AutoValue toString() includes all fields' values
  }

  /**
   * @param message The cause-message one might expect with a {@link java.lang.Exception}
   *     construction.
   */
  public IOException toIoException(String message) {
    return new IOException(String.format("%s: %s", message, toString()));
  }
}
