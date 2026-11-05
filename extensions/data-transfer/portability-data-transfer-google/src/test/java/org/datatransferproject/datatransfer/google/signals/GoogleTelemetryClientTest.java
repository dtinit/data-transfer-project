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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.FailureReasons;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleTelemetryClientTest {

  private static final String RECEIVER_URL =
      "https://receiver.example.com/v1:reportTransferTelemetry";
  private static final String TOKEN_URL = "https://oauth2.example.com/token";
  private static final String JOB_ID = "8b0c4c7e-3f0a-4d4e-9a52-1d3c3b6f2a10";
  /** 2026-10-15T14:00:00Z. */
  private static final long START_MILLIS = 1_792_072_800_000L;
  private static final JobLifeCycle COMPLETED =
      JobLifeCycle.builder()
          .setState(State.ENDED)
          .setEndReason(EndReason.SUCCESSFULLY_COMPLETED)
          .build();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final BooleanSupplier ALWAYS = () -> true;

  private final FakeServer server = new FakeServer();
  private final List<Long> sleeps = new ArrayList<>();
  private final List<String> infoLogs = new ArrayList<>();
  private final List<String> debugLogs = new ArrayList<>();
  private final Monitor monitor =
      new Monitor() {
        @Override
        public void info(Supplier<String> message, Object... data) {
          infoLogs.add(message.get());
        }

        @Override
        public void debug(Supplier<String> message, Object... data) {
          debugLogs.add(message.get());
        }
      };
  private final TokensAndUrlAuthData authData =
      new TokensAndUrlAuthData("access-token", "refresh-token", TOKEN_URL);
  private final GoogleTelemetryClient client =
      new GoogleTelemetryClient(
          new GoogleCredentialFactory(
              server,
              GsonFactory.getDefaultInstance(),
              new AppCredentials("key", "secret"),
              monitor),
          RECEIVER_URL,
          sleeps::add,
          monitor);

  // The expected JSON below is what the receiver's own tests parse.

  @Test
  void toJson_start_isTheJsonTheReceiverExpects() throws Exception {
    SignalRequest start =
        request(JobLifeCycle.JOB_STARTED())
            .setStartTelemetry(
                StartTelemetry.builder()
                    .setTimestamp(START_MILLIS)
                    .setEstimatedTransferSize(1_073_741_824L)
                    .setEstimatedNumberOfItems(500L)
                    .setExpirationTimestamp(START_MILLIS + Duration.ofDays(7).toMillis())
                    .setRecurringJobId("4a1e0f8d-7c2b-4f5e-8d3a-6b9c0e1f2a3b")
                    .build())
            .build();

    assertJson(
        start,
        "{\"jobId\": \"8b0c4c7e-3f0a-4d4e-9a52-1d3c3b6f2a10\","
            + " \"dataType\": \"PHOTOS\","
            + " \"jobStatus\":"
            + "   {\"state\": \"STARTED\", \"failureReason\": null, \"endReason\": null},"
            + " \"exportingService\": \"FACEBOOK\","
            + " \"importingService\": \"GOOGLE\","
            + " \"startTelemetry\": {"
            + "   \"timestamp\": \"2026-10-15T14:00:00Z\","
            + "   \"estimatedTransferSize\": 1073741824,"
            + "   \"estimatedNumberOfItems\": 500,"
            + "   \"expirationTimestamp\": \"2026-10-22T14:00:00Z\","
            + "   \"recurringJobId\": \"4a1e0f8d-7c2b-4f5e-8d3a-6b9c0e1f2a3b\"}}");
  }

  @Test
  void toJson_progress_isTheJsonTheReceiverExpects() throws Exception {
    SignalRequest progress =
        request(JobLifeCycle.IN_PROGRESS())
            .setProgressTelemetry(
                ProgressTelemetry.builder()
                    .setTimestamp(START_MILLIS + Duration.ofMinutes(5).toMillis())
                    .setTotalItemsFetched(120L)
                    .setTotalItemsFailedToFetch(0L)
                    .build())
            .build();

    assertJson(
        progress,
        "{\"jobId\": \"8b0c4c7e-3f0a-4d4e-9a52-1d3c3b6f2a10\","
            + " \"dataType\": \"PHOTOS\","
            + " \"jobStatus\":"
            + "   {\"state\": \"IN_PROGRESS\", \"failureReason\": null, \"endReason\": null},"
            + " \"exportingService\": \"FACEBOOK\","
            + " \"importingService\": \"GOOGLE\","
            + " \"progressTelemetry\": {"
            + "   \"timestamp\": \"2026-10-15T14:05:00Z\","
            + "   \"totalItemsFetched\": 120,"
            + "   \"totalItemsFailedToFetch\": 0}}");
  }

  @Test
  void toJson_end_isTheJsonTheReceiverExpects() throws Exception {
    JobLifeCycle partial =
        JobLifeCycle.builder()
            .setState(State.ENDED)
            .setEndReason(EndReason.PARTIALLY_COMPLETED)
            .setFailureReason(FailureReasons.DESTINATION_FULL)
            .build();
    SignalRequest end =
        request(partial)
            .setEndTelemetry(
                EndTelemetry.builder()
                    .setTimestamp(START_MILLIS + Duration.ofHours(1).toMillis())
                    .setTotalItemsListed(500L)
                    .setTotalItemsFetched(495L)
                    .setTotalBytesReceived(1_048_576_000L)
                    .setTotalItemsFailedToList(0L)
                    .setTotalItemsFailedToFetch(5L)
                    .build())
            .build();

    assertJson(
        end,
        "{\"jobId\": \"8b0c4c7e-3f0a-4d4e-9a52-1d3c3b6f2a10\","
            + " \"dataType\": \"PHOTOS\","
            + " \"jobStatus\": {"
            + "   \"state\": \"ENDED\","
            + "   \"endReason\": \"PARTIALLY_COMPLETED\","
            + "   \"failureReason\": \"DESTINATION_FULL\"},"
            + " \"exportingService\": \"FACEBOOK\","
            + " \"importingService\": \"GOOGLE\","
            + " \"endTelemetry\": {"
            + "   \"timestamp\": \"2026-10-15T15:00:00Z\","
            + "   \"totalItemsListed\": 500,"
            + "   \"totalItemsFetched\": 495,"
            + "   \"totalBytesReceived\": 1048576000,"
            + "   \"totalItemsFailedToList\": 0,"
            + "   \"totalItemsFailedToFetch\": 5}}");
  }

  @Test
  void toJson_keepsMilliseconds() throws Exception {
    JsonNode json = MAPPER.readTree(GoogleTelemetryClient.toJson(progressAt(START_MILLIS + 123)));

    assertThat(json.get("progressTelemetry").get("timestamp").asText())
        .isEqualTo("2026-10-15T14:00:00.123Z");
  }

  @Test
  void toJson_endWithoutTelemetry_isTheRequestsOwnJson() throws Exception {
    SignalRequest end = request(COMPLETED).build();

    assertThat(MAPPER.readTree(GoogleTelemetryClient.toJson(end)))
        .isEqualTo(MAPPER.readTree(MAPPER.writeValueAsBytes(end)));
  }

  @Test
  void send_postsTheJson_withTheUsersToken() throws Exception {
    SignalRequest start = startAt(START_MILLIS);

    client.send(start, authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(1);
    RecordedRequest sent = server.receiverRequests.get(0);
    assertThat(sent.method).isEqualTo("POST");
    assertThat(sent.url).isEqualTo(RECEIVER_URL);
    assertThat(sent.authorization).isEqualTo("Bearer access-token");
    assertThat(sent.contentType).isEqualTo("application/json");
    assertThat(MAPPER.readTree(sent.body))
        .isEqualTo(MAPPER.readTree(GoogleTelemetryClient.toJson(start)));
    assertThat(server.tokenRefreshes).isEqualTo(0);
    assertThat(infoLogs).isEmpty();
  }

  @Test
  void send_expiredToken_isRefreshedOnce_andTheRequestSentAgain() {
    server.receiverResponses.add(401);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.tokenRefreshes).isEqualTo(1);
    assertThat(authorizations())
        .containsExactly("Bearer access-token", "Bearer refreshed-token")
        .inOrder();
    assertThat(sleeps).isEmpty();
    assertThat(infoLogs).isEmpty();
  }

  @Test
  void send_laterRequests_reuseTheRefreshedToken() {
    server.receiverResponses.add(401);

    client.send(startAt(START_MILLIS), authData, ALWAYS);
    client.send(progressAt(START_MILLIS + 1), authData, ALWAYS);

    assertThat(server.tokenRefreshes).isEqualTo(1);
    assertThat(authorizations())
        .containsExactly("Bearer access-token", "Bearer refreshed-token", "Bearer refreshed-token")
        .inOrder();
  }

  @Test
  void send_stillUnauthorizedAfterTheRefresh_givesUp() {
    server.receiverResponses.add(401);
    server.receiverResponses.add(401);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(2);
    assertThat(server.tokenRefreshes).isEqualTo(1);
    assertThat(sleeps).isEmpty();
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("HTTP 401");
  }

  @Test
  void send_tokenCantBeRefreshed_givesUp() {
    server.tokenStatus = 400;
    server.receiverResponses.add(401);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(server.tokenRefreshes).isEqualTo(1);
    assertThat(sleeps).isEmpty();
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("the token can't be renewed");
  }

  @Test
  void send_refreshThatFailsWithANetworkError_isTriedAgainWithTheNextAttempt() {
    server.receiverResponses.add(401);
    server.receiverResponses.add(401);
    server.tokenResponses.add(new IOException("connection reset"));

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.tokenRefreshes).isEqualTo(2);
    assertThat(authorizations())
        .containsExactly("Bearer access-token", "Bearer access-token", "Bearer refreshed-token")
        .inOrder();
    assertThat(sleeps).containsExactly(1_000L);
    assertThat(infoLogs).isEmpty();
  }

  @Test
  void send_refreshThatFailsWithAServerError_isTriedAgainWithTheNextAttempt() {
    server.receiverResponses.add(401);
    server.receiverResponses.add(401);
    server.tokenResponses.add(503);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.tokenRefreshes).isEqualTo(2);
    assertThat(authorizations())
        .containsExactly("Bearer access-token", "Bearer access-token", "Bearer refreshed-token")
        .inOrder();
    assertThat(sleeps).containsExactly(1_000L);
    assertThat(infoLogs).isEmpty();
  }

  @Test
  void send_serverErrors_areSentThreeTimes_withBackoff() {
    server.receiverResponses.add(503);
    server.receiverResponses.add(500);
    server.receiverResponses.add(502);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(GoogleTelemetryClient.MAX_ATTEMPTS);
    assertThat(sleeps).containsExactly(1_000L, 2_000L).inOrder();
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("HTTP 502");
  }

  @Test
  void send_tooManyRequests_isSentAgain() {
    server.receiverResponses.add(429);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(2);
    assertThat(sleeps).containsExactly(1_000L);
    assertThat(infoLogs).isEmpty();
  }

  @Test
  void send_isntSentAgain_whenTheCallerDoesntAllowRetries() {
    server.receiverResponses.add(503);

    client.send(startAt(START_MILLIS), authData, () -> false);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(sleeps).isEmpty();
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("HTTP 503");
  }

  @Test
  void send_isntSentAgain_whenTheCallerStopsAllowingRetriesDuringTheBackoff() {
    server.receiverResponses.add(503);
    Iterator<Boolean> mayRetry = Arrays.asList(true, false).iterator();

    client.send(startAt(START_MILLIS), authData, mayRetry::next);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(sleeps).containsExactly(1_000L);
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("HTTP 503");
  }

  @Test
  void send_networkError_isSentAgain() {
    server.receiverResponses.add(new IOException("connection reset"));

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(2);
    assertThat(sleeps).containsExactly(1_000L);
    assertThat(infoLogs).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 404})
  void send_clientErrors_arentSentAgain(int status) {
    server.receiverResponses.add(status);

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(sleeps).isEmpty();
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("HTTP " + status);
  }

  @Test
  void send_progressFailures_areLoggedAtDebug_othersAtInfo() {
    server.receiverResponses.add(403);
    server.receiverResponses.add(403);

    client.send(startAt(START_MILLIS), authData, ALWAYS);
    client.send(progressAt(START_MILLIS + 1), authData, ALWAYS);

    assertThat(infoLogs).hasSize(1);
    assertThat(debugLogs).hasSize(1);
  }

  @Test
  void send_failure_logsTheSignal_andTheReceiversError() {
    server.receiverResponses.add(
        json(
            403,
            "{\"error\": {\"code\": 403,"
                + " \"message\": \"Request had insufficient authentication scopes.\","
                + " \"status\": \"PERMISSION_DENIED\"}}"));

    client.send(endAt(START_MILLIS), authData, ALWAYS);

    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0))
        .startsWith("Couldn't send transfer telemetry End to Google: HTTP 403: {\"error\"");
    assertThat(infoLogs.get(0)).contains("PERMISSION_DENIED");
    assertThat(infoLogs.get(0)).contains("Request had insufficient authentication scopes.");
  }

  @Test
  void send_failure_logsOnlyTheStartOfALongErrorResponse() {
    int limit = GoogleTelemetryClient.MAX_LOGGED_ERROR_BYTES;
    server.receiverResponses.add(json(400, "x".repeat(5 * limit)));

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).endsWith("x...");
    assertThat(infoLogs.get(0).length()).isLessThan(limit + 100);
  }

  @Test
  void send_setsTheConnectAndReadTimeouts() {
    // Three attempts with these timeouts, plus the backoff, fit in End's 60-second wait.
    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(server.receiverRequests.get(0).connectTimeoutMillis).isEqualTo(5_000);
    assertThat(server.receiverRequests.get(0).readTimeoutMillis).isEqualTo(12_000);
  }

  @Test
  void send_otherAuthData_usesItsOwnToken() {
    TokensAndUrlAuthData otherAuthData =
        new TokensAndUrlAuthData("other-access-token", "other-refresh-token", TOKEN_URL);

    client.send(startAt(START_MILLIS), authData, ALWAYS);
    client.send(startAt(START_MILLIS), otherAuthData, ALWAYS);

    assertThat(authorizations())
        .containsExactly("Bearer access-token", "Bearer other-access-token")
        .inOrder();
  }

  @Test
  void send_unexpectedException_isLogged_andNotThrown() {
    server.receiverResponses.add(new IllegalStateException("broken transport"));

    client.send(startAt(START_MILLIS), authData, ALWAYS);

    assertThat(server.receiverRequests).hasSize(1);
    assertThat(infoLogs).hasSize(1);
  }

  private List<String> authorizations() {
    return server.receiverRequests.stream()
        .map(request -> request.authorization)
        .collect(Collectors.toList());
  }

  private static void assertJson(SignalRequest request, String expected) throws IOException {
    assertThat(MAPPER.readTree(GoogleTelemetryClient.toJson(request)))
        .isEqualTo(MAPPER.readTree(expected));
  }

  private static SignalRequest.Builder request(JobLifeCycle jobStatus) {
    return SignalRequest.builder()
        .setJobId(JOB_ID)
        .setDataType("PHOTOS")
        .setJobStatus(jobStatus)
        .setExportingService("FACEBOOK")
        .setImportingService("GOOGLE");
  }

  private static SignalRequest startAt(long millis) {
    return request(JobLifeCycle.JOB_STARTED())
        .setStartTelemetry(StartTelemetry.builder().setTimestamp(millis).build())
        .build();
  }

  private static SignalRequest endAt(long millis) {
    return request(COMPLETED)
        .setEndTelemetry(
            EndTelemetry.builder()
                .setTimestamp(millis)
                .setTotalItemsFetched(1L)
                .setTotalItemsFailedToFetch(0L)
                .build())
        .build();
  }

  private static SignalRequest progressAt(long millis) {
    return request(JobLifeCycle.IN_PROGRESS())
        .setProgressTelemetry(
            ProgressTelemetry.builder()
                .setTimestamp(millis)
                .setTotalItemsFetched(1L)
                .setTotalItemsFailedToFetch(0L)
                .build())
        .build();
  }

  private static MockLowLevelHttpResponse json(int status, String content) {
    return new MockLowLevelHttpResponse()
        .setStatusCode(status)
        .setContentType("application/json; charset=UTF-8")
        .setContent(content);
  }

  private static String body(LowLevelHttpRequest request) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    request.getStreamingContent().writeTo(out);
    return new String(out.toByteArray(), UTF_8);
  }

  /** A request that reached the receiver. */
  private static final class RecordedRequest {
    final String method;
    final String url;
    final String authorization;
    final String contentType;
    final String body;
    final int connectTimeoutMillis;
    final int readTimeoutMillis;

    RecordedRequest(
        String method,
        String url,
        String authorization,
        String contentType,
        String body,
        int connectTimeoutMillis,
        int readTimeoutMillis) {
      this.method = method;
      this.url = url;
      this.authorization = authorization;
      this.contentType = contentType;
      this.body = body;
      this.connectTimeoutMillis = connectTimeoutMillis;
      this.readTimeoutMillis = readTimeoutMillis;
    }
  }

  /**
   * Answers token refreshes with the queued responses, then with {@code tokenStatus}, and answers
   * the receiver's requests with the queued responses, then with HTTP 200. A queued receiver
   * response is a status code, a full response, or an exception to throw.
   */
  private static final class FakeServer extends MockHttpTransport {
    final List<RecordedRequest> receiverRequests = new ArrayList<>();
    /** HTTP status codes, or exceptions to throw, for the receiver's next requests. */
    final Deque<Object> receiverResponses = new ArrayDeque<>();
    /** HTTP status codes, or exceptions to throw, for the next token refreshes. */
    final Deque<Object> tokenResponses = new ArrayDeque<>();
    int tokenStatus = 200;
    int tokenRefreshes;

    @Override
    public LowLevelHttpRequest buildRequest(String method, String url) {
      return new MockLowLevelHttpRequest(url) {
        private int connectTimeoutMillis;
        private int readTimeoutMillis;

        @Override
        public void setTimeout(int connectTimeout, int readTimeout) {
          connectTimeoutMillis = connectTimeout;
          readTimeoutMillis = readTimeout;
        }

        @Override
        public LowLevelHttpResponse execute() throws IOException {
          if (url.equals(TOKEN_URL)) {
            tokenRefreshes++;
            Object response = tokenResponses.isEmpty() ? tokenStatus : tokenResponses.removeFirst();
            if (response instanceof IOException) {
              throw (IOException) response;
            }
            int status = (Integer) response;
            if (status == 200) {
              return json(
                  200,
                  "{\"access_token\": \"refreshed-token\", \"expires_in\": 3600,"
                      + " \"token_type\": \"Bearer\"}");
            }
            // HTTP 400 is how the token server rejects a refresh token that's no longer valid.
            String error = status == 400 ? "invalid_grant" : "internal_failure";
            return json(status, "{\"error\": \"" + error + "\"}");
          }
          receiverRequests.add(
              new RecordedRequest(
                  method,
                  url,
                  getFirstHeaderValue("Authorization"),
                  getContentType(),
                  body(this),
                  connectTimeoutMillis,
                  readTimeoutMillis));
          Object response = receiverResponses.isEmpty() ? 200 : receiverResponses.removeFirst();
          if (response instanceof IOException) {
            throw (IOException) response;
          }
          if (response instanceof RuntimeException) {
            throw (RuntimeException) response;
          }
          if (response instanceof LowLevelHttpResponse) {
            return (LowLevelHttpResponse) response;
          }
          return json((Integer) response, "{}");
        }
      };
    }
  }
}
