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

package org.datatransferproject.datatransfer.google.signals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.util.Sleeper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Sends transfer telemetry to Google's receiver, the {@code ReportTransferTelemetry} method of the
 * Data Portability API, authorized by the user's Google Photos import token. That token has to
 * carry a scope that the receiver accepts, {@code photoslibrary.appendonly}: see {@link
 * GoogleSignalHandler}.
 *
 * <p>The request body is the {@link SignalRequest}'s JSON, with its timestamps as RFC 3339 strings
 * instead of epoch milliseconds, since the receiver rejects numbers there. A request that fails
 * with HTTP 429, a 5xx error or a network error is sent up to {@value #MAX_ATTEMPTS} times, 1 and
 * then 2 seconds apart, while the caller allows it. On HTTP 401 the token is refreshed and the
 * request sent again; a refresh that fails with a network or server error is tried again with the
 * next attempt. Other errors aren't retried. A failure is logged with the signal's name and the
 * start of the receiver's error response, which says why the request was rejected.
 *
 * <p>Each attempt has a 5-second connect timeout and a 12-second read timeout, so three attempts
 * fit in the 60 seconds that {@link GoogleSignalHandler} waits for End. A token refresh uses the
 * HTTP transport's own timeouts, so an attempt that refreshes can take longer, but the handler's
 * wait still bounds how long the worker waits.
 */
final class GoogleTelemetryClient implements TelemetrySender {

  /** The receiver's endpoint. */
  static final String TELEMETRY_URL =
      "https://dataportability.googleapis.com/v1:reportTransferTelemetry";

  /** The most times one request is sent. */
  static final int MAX_ATTEMPTS = 3;

  /** The most of a failed response's body that's logged, in bytes. */
  static final int MAX_LOGGED_ERROR_BYTES = 1_000;

  private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
  // A little longer than the receiver's own 10-second deadline, so that when the receiver times
  // out, its HTTP 504 arrives before the read times out.
  private static final int READ_TIMEOUT_MILLIS = 12_000;
  private static final long BACKOFF_MILLIS = 1_000;
  private static final int TOO_MANY_REQUESTS = 429;
  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final GoogleCredentialFactory credentialFactory;
  private final GenericUrl url;
  private final Sleeper sleeper;
  private final Monitor monitor;

  // The credential for the latest request's auth data, so that a refreshed token is reused.
  // Guarded by this.
  @Nullable private TokensAndUrlAuthData cachedAuthData;
  @Nullable private Credential cachedCredential;

  GoogleTelemetryClient(GoogleCredentialFactory credentialFactory, Monitor monitor) {
    this(credentialFactory, TELEMETRY_URL, Sleeper.DEFAULT, monitor);
  }

  @VisibleForTesting
  GoogleTelemetryClient(
      GoogleCredentialFactory credentialFactory, String url, Sleeper sleeper, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.url = new GenericUrl(url);
    this.sleeper = sleeper;
    this.monitor = monitor;
  }

  @Override
  public synchronized void send(
      SignalRequest request, TokensAndUrlAuthData authData, BooleanSupplier mayRetry) {
    String couldntSend =
        "Couldn't send transfer telemetry "
            + signalName(request)
            + " for job "
            + request.jobId()
            + " to Google";
    try {
      byte[] body = toJson(request);
      Credential credential = credentialFor(authData);
      boolean refreshed = false;
      String failure = "";
      for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        if (attempt > 1 && !waitToRetry(attempt, mayRetry)) {
          break;
        }
        try {
          Response response = post(body, credential);
          if (response.status == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED && !refreshed) {
            // The token has expired: refresh it and send the request again. It counts as refreshed
            // only once the refresh works, so a refresh that fails is tried again next attempt.
            credentialFactory.refreshCredential(credential);
            refreshed = true;
            response = post(body, credential);
          }
          if (HttpStatusCodes.isSuccess(response.status)) {
            return;
          }
          failure = response.toString();
          if (!isRetryable(response.status)) {
            break;
          }
        } catch (TokenResponseException e) {
          failure = "refreshing the token failed with HTTP " + e.getStatusCode();
          if (e.getDetails() != null) {
            failure += ": " + e.getDetails().getError();
          }
          if (!isRetryable(e.getStatusCode())) {
            break;
          }
        } catch (IOException e) {
          // A network error, so send it again.
          failure = e.toString();
        }
      }
      String reason = failure;
      log(request, () -> couldntSend + ": " + reason);
    } catch (InvalidTokenException e) {
      log(request, () -> couldntSend + ": the token can't be renewed");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException | RuntimeException e) {
      log(request, () -> couldntSend + ".", e);
    }
  }

  /**
   * Waits before attempt number {@code attempt}, and returns whether to make it: only if the caller
   * still allows retries, before and after the wait.
   */
  private boolean waitToRetry(int attempt, BooleanSupplier mayRetry) throws InterruptedException {
    if (!mayRetry.getAsBoolean()) {
      return false;
    }
    sleeper.sleep(BACKOFF_MILLIS * (attempt - 1));
    return mayRetry.getAsBoolean();
  }

  /**
   * Returns the JSON that the receiver expects for {@code request}: the request's own JSON, with
   * its timestamps as RFC 3339 strings.
   */
  @VisibleForTesting
  static byte[] toJson(SignalRequest request) throws IOException {
    ObjectNode json = OBJECT_MAPPER.valueToTree(request);
    toRfc3339(json, "startTelemetry", "timestamp");
    toRfc3339(json, "startTelemetry", "expirationTimestamp");
    toRfc3339(json, "progressTelemetry", "timestamp");
    toRfc3339(json, "endTelemetry", "timestamp");
    return OBJECT_MAPPER.writeValueAsBytes(json);
  }

  /** Replaces the epoch milliseconds in {@code field} of the {@code block} object, if set. */
  private static void toRfc3339(ObjectNode json, String block, String field) {
    JsonNode telemetry = json.get(block);
    if (telemetry instanceof ObjectNode && telemetry.path(field).isIntegralNumber()) {
      long millis = telemetry.get(field).asLong();
      ((ObjectNode) telemetry).put(field, Instant.ofEpochMilli(millis).toString());
    }
  }

  /** The signal's name in logs: Start, Progress or End. */
  private static String signalName(SignalRequest request) {
    if (request.startTelemetry() != null) {
      return "Start";
    }
    if (request.progressTelemetry() != null) {
      return "Progress";
    }
    return "End";
  }

  private Credential credentialFor(TokensAndUrlAuthData authData) {
    // The worker passes the same auth data with every signal for a job.
    if (cachedCredential == null || authData != cachedAuthData) {
      cachedCredential = credentialFactory.createCredential(authData);
      cachedAuthData = authData;
    }
    return cachedCredential;
  }

  private Response post(byte[] body, Credential credential) throws IOException {
    HttpRequest request =
        credentialFactory
            .getHttpTransport()
            .createRequestFactory()
            .buildPostRequest(url, new ByteArrayContent(JSON_MEDIA_TYPE, body));
    request.getHeaders().setAuthorization("Bearer " + credential.getAccessToken());
    request.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    request.setReadTimeout(READ_TIMEOUT_MILLIS);
    // send() decides what to retry.
    request.setNumberOfRetries(0);
    request.setThrowExceptionOnExecuteError(false);
    HttpResponse response = request.execute();
    try {
      int status = response.getStatusCode();
      return new Response(status, HttpStatusCodes.isSuccess(status) ? "" : errorBody(response));
    } finally {
      response.disconnect();
    }
  }

  /**
   * Returns the start of a failed response's body, on one line, or an empty string if it can't be
   * read. For Google's API errors that's a JSON object with the error's status and message.
   */
  private static String errorBody(HttpResponse response) {
    try (InputStream content = response.getContent()) {
      if (content == null) {
        return "";
      }
      // One byte past the limit, to tell whether the body was cut short.
      byte[] bytes =
          ByteStreams.toByteArray(ByteStreams.limit(content, MAX_LOGGED_ERROR_BYTES + 1));
      boolean cut = bytes.length > MAX_LOGGED_ERROR_BYTES;
      int length = cut ? MAX_LOGGED_ERROR_BYTES : bytes.length;
      String text = new String(bytes, 0, length, response.getContentCharset());
      text = CharMatcher.whitespace().trimAndCollapseFrom(text, ' ');
      return cut ? text + "..." : text;
    } catch (IOException | RuntimeException e) {
      return "";
    }
  }

  private static boolean isRetryable(int status) {
    return status == TOO_MANY_REQUESTS || status >= HttpStatusCodes.STATUS_CODE_SERVER_ERROR;
  }

  /**
   * Logs at info, except a Progress failure at debug: Progress repeats every interval, so its
   * failures would too.
   */
  private void log(SignalRequest request, Supplier<String> message, Object... data) {
    if (request.progressTelemetry() != null) {
      monitor.debug(message, data);
    } else {
      monitor.info(message, data);
    }
  }

  /** A response's status, and for a failure, the start of its body. */
  private static final class Response {
    private final int status;
    private final String errorBody;

    private Response(int status, String errorBody) {
      this.status = status;
      this.errorBody = errorBody;
    }

    @Override
    public String toString() {
      return errorBody.isEmpty() ? "HTTP " + status : "HTTP " + status + ": " + errorBody;
    }
  }
}
