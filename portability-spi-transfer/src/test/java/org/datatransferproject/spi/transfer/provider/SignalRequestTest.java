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

package org.datatransferproject.spi.transfer.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.datatransferproject.spi.transfer.types.FailureReasons;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;
import org.junit.jupiter.api.Test;

/**
 * Guards the JSON that existing signal handlers (e.g. Apple, which serializes the whole request
 * with a default {@link ObjectMapper}) send to partner backends. These payloads must not change.
 */
public class SignalRequestTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String JOB_ID = "6b7b1d3a-4c57-4c3e-9d1b-2a3f4e5d6c7b";

  /** The JSON a STARTED signal serialized to before telemetry was added. */
  private static final String LEGACY_STARTED_JSON =
      "{\"jobId\":\"" + JOB_ID + "\",\"dataType\":\"PHOTOS\","
          + "\"jobStatus\":{\"state\":\"STARTED\",\"failureReason\":null,\"endReason\":null},"
          + "\"exportingService\":\"APPLE\",\"importingService\":\"GOOGLE\"}";

  /** The JSON an ENDED signal with a failure reason serialized to before telemetry was added. */
  private static final String LEGACY_ENDED_JSON =
      "{\"jobId\":\"" + JOB_ID + "\",\"dataType\":\"PHOTOS\","
          + "\"jobStatus\":{\"state\":\"ENDED\",\"failureReason\":\"DESTINATION_FULL\","
          + "\"endReason\":\"PARTIALLY_COMPLETED\"},"
          + "\"exportingService\":\"APPLE\",\"importingService\":\"GOOGLE\"}";

  @Test
  public void legacyStartedRequest_serializesExactlyAsBefore() throws Exception {
    SignalRequest request = baseRequest().setJobStatus(JobLifeCycle.JOB_STARTED()).build();

    assertJsonEquals(LEGACY_STARTED_JSON, MAPPER.writeValueAsString(request));
  }

  @Test
  public void legacyEndedRequest_serializesExactlyAsBefore() throws Exception {
    SignalRequest request =
        baseRequest()
            .setJobStatus(
                JobLifeCycle.builder()
                    .setState(JobLifeCycle.State.ENDED)
                    .setEndReason(EndReason.PARTIALLY_COMPLETED)
                    .setFailureReason(FailureReasons.DESTINATION_FULL)
                    .build())
            .build();

    assertJsonEquals(LEGACY_ENDED_JSON, MAPPER.writeValueAsString(request));
  }

  @Test
  public void startTelemetry_isAddedAsItsOwnBlock_andLegacyKeysAreUnchanged() throws Exception {
    SignalRequest request =
        baseRequest()
            .setJobStatus(JobLifeCycle.JOB_STARTED())
            .setStartTelemetry(
                StartTelemetry.builder().setTimestamp(1000L).setExpirationTimestamp(2000L).build())
            .build();

    String expected =
        LEGACY_STARTED_JSON.substring(0, LEGACY_STARTED_JSON.length() - 1)
            + ",\"startTelemetry\":{\"timestamp\":1000,\"expirationTimestamp\":2000}}";
    assertJsonEquals(expected, MAPPER.writeValueAsString(request));
  }

  @Test
  public void progressTelemetry_isAddedAsItsOwnBlock() throws Exception {
    SignalRequest request =
        baseRequest()
            .setJobStatus(JobLifeCycle.IN_PROGRESS())
            .setProgressTelemetry(
                ProgressTelemetry.builder()
                    .setTimestamp(3000L)
                    .setTotalItemsFetched(10L)
                    .setTotalItemsFailedToFetch(1L)
                    .build())
            .build();

    JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(request));
    assertEquals("IN_PROGRESS", json.get("jobStatus").get("state").asText());
    assertEquals(10L, json.get("progressTelemetry").get("totalItemsFetched").asLong());
    assertFalse(json.has("startTelemetry"));
    assertFalse(json.has("endTelemetry"));
  }

  @Test
  public void endTelemetry_isAddedAsItsOwnBlock_andLegacyKeysAreUnchanged() throws Exception {
    SignalRequest request =
        baseRequest()
            .setJobStatus(
                JobLifeCycle.builder()
                    .setState(JobLifeCycle.State.ENDED)
                    .setEndReason(EndReason.PARTIALLY_COMPLETED)
                    .setFailureReason(FailureReasons.DESTINATION_FULL)
                    .build())
            .setEndTelemetry(
                EndTelemetry.builder()
                    .setTimestamp(4000L)
                    .setTotalItemsFetched(98L)
                    .setTotalItemsFailedToFetch(2L)
                    .build())
            .build();

    String expected =
        LEGACY_ENDED_JSON.substring(0, LEGACY_ENDED_JSON.length() - 1)
            + ",\"endTelemetry\":{\"timestamp\":4000,\"totalItemsFetched\":98,"
            + "\"totalItemsFailedToFetch\":2}}";
    assertJsonEquals(expected, MAPPER.writeValueAsString(request));
  }

  static SignalRequest.Builder baseRequest() {
    return SignalRequest.builder()
        .setJobId(JOB_ID)
        .setDataType("PHOTOS")
        .setExportingService("APPLE")
        .setImportingService("GOOGLE");
  }

  /**
   * Compares JSON trees: the same keys (including explicit nulls) and values. Key order is not
   * significant in JSON, so it isn't compared.
   */
  static void assertJsonEquals(String expectedJson, String actualJson) throws Exception {
    JsonNode expected = MAPPER.readTree(expectedJson);
    JsonNode actual = MAPPER.readTree(actualJson);
    assertEquals(expected, actual, "Actual JSON: " + actualJson);
  }
}
