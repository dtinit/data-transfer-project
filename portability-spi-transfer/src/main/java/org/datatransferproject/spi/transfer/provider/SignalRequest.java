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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;

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

    public abstract SignalRequest build();
  }
}
