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
 * A periodic snapshot of transfer progress, sent with {@link JobLifeCycle.State#IN_PROGRESS}
 * signals while data is being copied.
 *
 * <p>Only sent to signal handlers that opt in to telemetry (see {@code
 * SignalHandler#supportsTelemetry()}). Unset optional fields are omitted from the JSON.
 *
 * <p>An item counts as failed only after a terminal failure, once all retries are exhausted.
 * Transient errors that later succeed are not counted.
 */
@AutoValue
@JsonDeserialize(builder = ProgressTelemetry.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProgressTelemetry {

  public static Builder builder() {
    return new AutoValue_ProgressTelemetry.Builder();
  }

  /** When this snapshot was taken, in epoch milliseconds. */
  @JsonProperty("timestamp")
  public abstract Long timestamp();

  /** Items found in the origin so far, if the exporter reports it. */
  @Nullable
  @JsonProperty("totalItemsListed")
  public abstract Long totalItemsListed();

  /** Items successfully copied to the destination so far. */
  @JsonProperty("totalItemsFetched")
  public abstract Long totalItemsFetched();

  /** Bytes copied to the destination so far, if known. */
  @Nullable
  @JsonProperty("totalBytesReceived")
  public abstract Long totalBytesReceived();

  /**
   * Items the exporter couldn't enumerate. Absent means "not reported", which is different from 0.
   */
  @Nullable
  @JsonProperty("totalItemsFailedToList")
  public abstract Long totalItemsFailedToList();

  /** Items that failed terminally while being downloaded from the origin or uploaded. */
  @JsonProperty("totalItemsFailedToFetch")
  public abstract Long totalItemsFailedToFetch();

  @AutoValue.Builder
  // Builder-based deserialization reads this from the builder, not the value class. Ignoring
  // unknown fields lets future versions add fields without breaking readers of these types.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public abstract static class Builder {
    @JsonCreator
    private static Builder create() {
      return ProgressTelemetry.builder();
    }

    @JsonProperty("timestamp")
    public abstract Builder setTimestamp(Long timestamp);

    @JsonProperty("totalItemsListed")
    public abstract Builder setTotalItemsListed(@Nullable Long totalItemsListed);

    @JsonProperty("totalItemsFetched")
    public abstract Builder setTotalItemsFetched(Long totalItemsFetched);

    @JsonProperty("totalBytesReceived")
    public abstract Builder setTotalBytesReceived(@Nullable Long totalBytesReceived);

    @JsonProperty("totalItemsFailedToList")
    public abstract Builder setTotalItemsFailedToList(@Nullable Long totalItemsFailedToList);

    @JsonProperty("totalItemsFailedToFetch")
    public abstract Builder setTotalItemsFailedToFetch(Long totalItemsFailedToFetch);

    public abstract ProgressTelemetry build();
  }
}
