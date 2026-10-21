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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * Telemetry sent with the {@link JobLifeCycle.State#STARTED} signal, before any data is copied.
 *
 * <p>Only sent to signal handlers that opt in to telemetry (see {@code
 * SignalHandler#supportsTelemetry()}). Unset optional fields are omitted from the JSON.
 */
@AutoValue
@JsonDeserialize(builder = StartTelemetry.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class StartTelemetry {

  public static Builder builder() {
    return new AutoValue_StartTelemetry.Builder();
  }

  /** When the worker started the job, in epoch milliseconds. */
  @JsonProperty("timestamp")
  public abstract Long timestamp();

  /** Estimated size of the data to transfer, in bytes, if the exporter can compute it. */
  @Nullable
  @JsonProperty("estimatedTransferSize")
  public abstract Long estimatedTransferSize();

  /** Estimated number of items to transfer, if the exporter can compute it. */
  @Nullable
  @JsonProperty("estimatedNumberOfItems")
  public abstract Long estimatedNumberOfItems();

  /**
   * When the destination should consider the job dead if no end signal has arrived, in epoch
   * milliseconds.
   *
   * <p>Set by the origin service that runs the DTP worker, based on how long it allows a worker to
   * stay alive before stopping it. For example, if the origin stops workers after 7 days, this is
   * this signal's {@link #timestamp()} plus 7 days. A worker can't still be running after this
   * time, so a missing end signal means the job died.
   *
   * <p>A job can send more than one start signal, for example when it's resumed on a new worker.
   * Each start signal carries its own expiration, and the latest one applies.
   */
  @JsonProperty("expirationTimestamp")
  public abstract Long expirationTimestamp();

  /** The recurring job this transfer is one run of. Absent for one-off transfers. */
  @Nullable
  @JsonProperty("recurringJobId")
  public abstract String recurringJobId();

  @AutoValue.Builder
  // Builder-based deserialization reads this from the builder, not the value class. Ignoring
  // unknown fields lets future versions add fields without breaking readers of these types.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public abstract static class Builder {
    @JsonCreator
    private static Builder create() {
      return StartTelemetry.builder();
    }

    @JsonProperty("timestamp")
    public abstract Builder setTimestamp(Long timestamp);

    @JsonProperty("estimatedTransferSize")
    public abstract Builder setEstimatedTransferSize(@Nullable Long estimatedTransferSize);

    @JsonProperty("estimatedNumberOfItems")
    public abstract Builder setEstimatedNumberOfItems(@Nullable Long estimatedNumberOfItems);

    @JsonProperty("expirationTimestamp")
    public abstract Builder setExpirationTimestamp(Long expirationTimestamp);

    @JsonProperty("recurringJobId")
    public abstract Builder setRecurringJobId(@Nullable String recurringJobId);

    public abstract StartTelemetry build();
  }
}
