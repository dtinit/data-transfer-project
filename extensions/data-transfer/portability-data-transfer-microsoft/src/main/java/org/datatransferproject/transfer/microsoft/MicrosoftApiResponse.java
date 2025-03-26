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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import okhttp3.Response;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;

/**
 * API response from Microsoft API servers.
 *
 * <p>Example usage error handling workflow:
 *
 * <pre>{@code
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
 * }
 * resp.throwDtpException(); // or returnConvertDtpException might help
 * }</pre>
 */
@AutoValue
public abstract class MicrosoftApiResponse {
  @VisibleForTesting
  public static final String CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION =
      "unrecognized class of Microsoft API error";

  /** HTTP status code. */
  public abstract int httpStatus();

  /** HTTP status message. */
  public abstract String httpMessage();

  /** HTTP body of the response if any was present. */
  public abstract Optional<String> body();

  /** Exception that occurred when trying to read the HTTP body of the response. */
  public abstract Optional<IOException> bodyException();

  /**
   * Builds from key fields within an HTTP response, closing said response
   *
   * <p>If the body is a requirement and you need to fail-fast for bad response-bodies, then make
   * sure you're calling an introspective method that tries to actually utiilze the body, eg: {@link
   * getJsonValue} (or add a new one). By default, because bodies are not always provided or needed,
   * this construction method tries to save the response-body but stores an error in {@link
   * bodyException} (which will be included in the usual exception output via {@link
   * throwDtpException} and co).
   *
   * <p>Warning: this loads the entire response body to memory, as we assume all Microsoft API
   * responses we're dealing with are (at the most complex end) just JSON responses intended to be
   * parsed (as opposed to say, streams of an arbitrarily-large file's bytes).
   */
  public static MicrosoftApiResponse ofResponse(Response response) throws IOException {
    Builder builder =
        MicrosoftApiResponse.builder()
            .setHttpMessage(response.message())
            .setHttpStatus(response.code());

    if (response.body() != null) {
      final String body;
      try {
        body = response.body().string();
        if (!Strings.isNullOrEmpty(body)) {
          builder.setBody(body);
        }
      } catch (IOException e) {
        builder.setBodyException(e);
      }

      response.close(); // only close if we _had_ a body field to read
    }

    return builder.build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHttpStatus(int httpStatus);

    public abstract Builder setHttpMessage(String httpMessage);

    public abstract Builder setBody(String body);

    public abstract Builder setBodyException(IOException bodyException);

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
  // See
  // https://learn.microsoft.com/en-us/graph/resolve-auth-errors#401-unauthorized-error-is-your-token-valid
  // And Microsoft's Java SDK to interact with these APIs:
  // https://github.com/AzureAD/microsoft-authentication-library-for-java
  public boolean isTokenRefreshRequired() {
    return httpStatus() == 401 && bodyContains("InvalidAuthenticationToken");
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
  public void throwDtpException(String message)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    switch (toFatalState()) {
      case FATAL_STATE_FATAL_PERMISSION_DENIED:
        throw new PermissionDeniedException(
            "User access to microsoft onedrive was denied", toIoException(message));
      case FATAL_STATE_FATAL_DESTINATION_FULL:
        throw new DestinationMemoryFullException(
            "Microsoft destination storage limit reached", toIoException(message));
      case FATAL_STATE_FATAL_UNSPECIFIED:
        throw toIoException(String.format("%s: %s", CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION, message));
    }
    throw new AssertionError("exhaustive switch");
  }

  /**
   * "Return" that always throws an exception; just an ergonomic wrapper for {@link
   * throwDtpException} that gives you ability to use call this in a lexical scope that requires a
   * return.
   *
   * <p>Never returns, always causes an exception. Return signature is so you can include this in a
   * codepath that would otherwise excpect a `throw new` keyword to be present if you're missing a
   * final return.
   */
  public MicrosoftApiResponse returnConvertDtpException(String message)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    throwDtpException(message);
    // above always throws, but javac doesn't understand without a `throw new` directly visible
    throw new AssertionError(
        String.format(
            "bug: throwDtpException should have thrown for failed response state: %s", message));
  }

  private FatalState toFatalState() {
    checkState(isFatal(), "cannot explain fatal state when is apparently recoverable");
    if (httpStatus() == 403 && (bodyContains("accessDenied") || bodyContains("notAllowed"))) {
      return FatalState.FATAL_STATE_FATAL_PERMISSION_DENIED;
    }
    // Nit: we _could_ just parse the body into json properly and make sure the JSON body "message"
    // field has this string. This seems fine for now.
    if (httpStatus() == 507 && (bodyContains("Insufficient Space Available") || bodyContains("quotaLimitReached"))) {
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
   * Produce {@link toIoException} with a message used to construct the exception's cause.
   *
   * @param message The cause-message one might expect with a {@link java.lang.Exception}
   *     construction.
   */
  public IOException toIoException(String message) {
    return new IOException(String.format("%s: %s", message, toString()));
  }

  private IOException toIoException(String message, Exception e) {
    return new IOException(String.format("%s: %s", message, toString()), e);
  }

  /** Extracts a top-level JSON value, at key `jsonTopKey`, from current response body. */
  public String getJsonValue(ObjectMapper objectMapper, String jsonTopKey, String causeMessage)
      throws IOException {
    String responseBody = checkResponseBody(causeMessage);
    final Map<String, Object> json;
    try {
      // convert to a map, by reading entire body into memory
      json = objectMapper.readValue(responseBody, Map.class);
    } catch (IOException e) {
      throw toIoException(causeMessage, e);
    }
    checkState(
        json.containsKey(jsonTopKey),
        "response body missing top-level JSON field \"%s\"",
        jsonTopKey);
    final String jsonValue = (String) json.get(jsonTopKey);
    checkState(
        !Strings.isNullOrEmpty(jsonValue),
        "Expected JSON value for key \"%s\" to be present in in JSON body: %s",
        jsonTopKey,
        json);
    return jsonValue;
  }

  /** Returns response body or throws DTP base exception with `causeMessage`. */
  private String checkResponseBody(String causeMessage) throws IOException {
    return body()
        .orElseThrow(
            () ->
                toIoException(
                    String.format("HTTP response-body unexpectedly empty: %s", causeMessage)));
  }

  /** Reads body into memory and checks for needle. */
  private boolean bodyContains(String needle) {
    return body().isPresent() && body().get().contains(needle);
  }
}
