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

package org.datatransferproject.spi.transfer.types.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class TelemetryTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void startTelemetry_roundTripsAllFields() throws Exception {
    StartTelemetry start =
        StartTelemetry.builder()
            .setTimestamp(1000L)
            .setEstimatedTransferSize(5_000_000_000L)
            .setEstimatedNumberOfItems(1234L)
            .setExpirationTimestamp(2000L)
            .setRecurringJobId("a3c1e9f0-1111-2222-3333-444455556666")
            .build();

    String json = MAPPER.writeValueAsString(start);

    assertEquals(start, MAPPER.readValue(json, StartTelemetry.class));
  }

  @Test
  public void startTelemetry_omitsUnsetOptionalFields() throws Exception {
    StartTelemetry start =
        StartTelemetry.builder().setTimestamp(1000L).setExpirationTimestamp(2000L).build();

    assertEquals(
        MAPPER.readTree("{\"timestamp\":1000,\"expirationTimestamp\":2000}"),
        MAPPER.readTree(MAPPER.writeValueAsString(start)));
  }

  @Test
  public void startTelemetry_requiresTimestampAndExpiration() {
    assertThrows(
        IllegalStateException.class,
        () -> StartTelemetry.builder().setTimestamp(1000L).build());
    assertThrows(
        IllegalStateException.class,
        () -> StartTelemetry.builder().setExpirationTimestamp(2000L).build());
  }

  @Test
  public void progressTelemetry_roundTripsAllFields() throws Exception {
    ProgressTelemetry progress =
        ProgressTelemetry.builder()
            .setTimestamp(3000L)
            .setTotalItemsListed(500L)
            .setTotalItemsFetched(450L)
            .setTotalBytesReceived(9_000_000L)
            .setTotalItemsFailedToList(3L)
            .setTotalItemsFailedToFetch(7L)
            .build();

    String json = MAPPER.writeValueAsString(progress);

    assertEquals(progress, MAPPER.readValue(json, ProgressTelemetry.class));
  }

  @Test
  public void progressTelemetry_requiresTimestampFetchedAndFailedToFetch() {
    assertThrows(
        IllegalStateException.class,
        () -> ProgressTelemetry.builder().setTimestamp(1L).setTotalItemsFetched(1L).build());
    assertThrows(
        IllegalStateException.class,
        () -> ProgressTelemetry.builder().setTimestamp(1L).setTotalItemsFailedToFetch(1L).build());
    assertThrows(
        IllegalStateException.class,
        () ->
            ProgressTelemetry.builder()
                .setTotalItemsFetched(1L)
                .setTotalItemsFailedToFetch(1L)
                .build());
  }

  @Test
  public void endTelemetry_roundTripsAllFields() throws Exception {
    EndTelemetry end =
        EndTelemetry.builder()
            .setTimestamp(4000L)
            .setTotalItemsListed(500L)
            .setTotalItemsFetched(490L)
            .setTotalBytesReceived(9_800_000L)
            .setTotalItemsFailedToList(0L)
            .setTotalItemsFailedToFetch(10L)
            .build();

    String json = MAPPER.writeValueAsString(end);

    assertEquals(end, MAPPER.readValue(json, EndTelemetry.class));
  }

  @Test
  public void failedToList_absentMeansNotReported_zeroIsSent() throws Exception {
    EndTelemetry notReported =
        EndTelemetry.builder()
            .setTimestamp(4000L)
            .setTotalItemsFetched(1L)
            .setTotalItemsFailedToFetch(0L)
            .build();
    EndTelemetry reportedZero =
        EndTelemetry.builder()
            .setTimestamp(4000L)
            .setTotalItemsFetched(1L)
            .setTotalItemsFailedToFetch(0L)
            .setTotalItemsFailedToList(0L)
            .build();

    JsonNode notReportedJson = MAPPER.readTree(MAPPER.writeValueAsString(notReported));
    JsonNode reportedZeroJson = MAPPER.readTree(MAPPER.writeValueAsString(reportedZero));

    assertFalse(notReportedJson.has("totalItemsFailedToList"));
    assertTrue(reportedZeroJson.has("totalItemsFailedToList"));
    assertEquals(0L, reportedZeroJson.get("totalItemsFailedToList").asLong());
  }

  @Test
  public void deserialization_ignoresUnknownFields() throws Exception {
    // Lets future versions add fields without breaking Java readers of today's types.
    EndTelemetry end =
        MAPPER.readValue(
            "{\"timestamp\":1,\"totalItemsFetched\":2,\"totalItemsFailedToFetch\":3,"
                + "\"someFutureField\":\"x\"}",
            EndTelemetry.class);

    assertEquals(2L, end.totalItemsFetched());
  }
}
