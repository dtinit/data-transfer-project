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

import java.util.Objects;
import java.util.UUID;
import org.datatransferproject.spi.transfer.types.signals.SignalType;
import org.datatransferproject.types.common.models.DataVertical;

public class SignalRequest {
  private final String jobId;
  private final String dataType;
  private final String jobStatus;
  private final String exportingService;
  private final String importingService;


  public SignalRequest(String jobId, String dataType, String jobStatus, String exportService, String importService) {
    Objects.requireNonNull(jobId, "jobId cannot be null");
    Objects.requireNonNull(dataType, "dataType cannot be null");
    Objects.requireNonNull(jobStatus, "jobStatus cannot be null");
    Objects.requireNonNull(exportService, "exportService cannot be null");
    Objects.requireNonNull(importService, "importService cannot be null");

    this.jobId = jobId;
    this.dataType = dataType;
    this.jobStatus = jobStatus;
    this.exportingService = exportService;
    this.importingService = importService;
  }

  public String getJobId() {
    return jobId;
  }

  public String getDataType() {
    return dataType;
  }

  public String getJobStatus() {
    return jobStatus;
  }

  public String getExportingService() {
    return exportingService;
  }

  public String getImportingService() {
    return importingService;
  }

  public static SignalRequestBuilder newBuilder() {
    return new SignalRequestBuilder();
  }

  @Override
  public String toString() {
    return "SignalRequest{" +
      "jobId='" + jobId + '\'' +
      ", dataType='" + dataType + '\'' +
      ", jobStatus='" + jobStatus + '\'' +
      ", exportingService='" + exportingService + '\'' +
      ", importingService='" + importingService + '\'' +
      '}';
  }

  public static final class SignalRequestBuilder {
    private String jobId;
    private String dataType;
    private String jobStatus;
    private String exportingService;
    private String importingService;

    public SignalRequestBuilder withJobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public SignalRequestBuilder withDataType(String dataType) {
      this.dataType = dataType;
      return this;
    }

    public SignalRequestBuilder withJobStatus(String jobStatus) {
      this.jobStatus = jobStatus;
      return this;
    }

    public SignalRequestBuilder withExportingService(String exportingService) {
      this.exportingService = exportingService;
      return this;
    }

    public SignalRequestBuilder withImportingService(String importingService) {
      this.importingService = importingService;
      return this;
    }

    public SignalRequest build() {
      return new SignalRequest(jobId, dataType, jobStatus, exportingService, importingService);
    }
  }
}
