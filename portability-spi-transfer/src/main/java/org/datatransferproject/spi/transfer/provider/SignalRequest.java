/*
 * Copyright 2024 The Data Transfer Project Authors.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;

@AutoValue
@JsonDeserialize(builder = SignalRequest.Builder.class)
public abstract class SignalRequest {
  @JsonProperty("jobId")
  public abstract String jobId();

  @JsonProperty("dataType")
  public abstract String dataType();

  @JsonProperty("jobStatus")
  public abstract JobLifeCycle jobStatus();

  @JsonProperty("exportingService")
  public abstract String exportingService();

  @JsonProperty("importingService")
  public abstract String importingService();

  /**
   * Telemetry for a {@link JobLifeCycle.State#STARTED} signal. Only set for handlers that return
   * true from {@link SignalHandler#supportsTelemetry()}; omitted from the JSON when unset.
   */
  @Nullable
  @JsonProperty("startTelemetry")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract StartTelemetry startTelemetry();

  /**
   * Telemetry for a {@link JobLifeCycle.State#IN_PROGRESS} signal. Only set for handlers that
   * return true from {@link SignalHandler#supportsTelemetry()}; omitted from the JSON when unset.
   */
  @Nullable
  @JsonProperty("progressTelemetry")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract ProgressTelemetry progressTelemetry();

  /**
   * Telemetry for a {@link JobLifeCycle.State#ENDED} signal. Only set for handlers that return true
   * from {@link SignalHandler#supportsTelemetry()}; omitted from the JSON when unset.
   */
  @Nullable
  @JsonProperty("endTelemetry")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract EndTelemetry endTelemetry();

  public static Builder builder() {
    return new AutoValue_SignalRequest.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setJobId(String jobId);
    public abstract Builder setDataType(String dataType);
    public abstract Builder setJobStatus(JobLifeCycle jobStatus);
    public abstract Builder setExportingService(String exportingService);
    public abstract Builder setImportingService(String importingService);
    public abstract Builder setStartTelemetry(@Nullable StartTelemetry startTelemetry);
    public abstract Builder setProgressTelemetry(@Nullable ProgressTelemetry progressTelemetry);
    public abstract Builder setEndTelemetry(@Nullable EndTelemetry endTelemetry);

    public abstract SignalRequest build();
  }
}
