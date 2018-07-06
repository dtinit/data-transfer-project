/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.dataportabilityproject.transfer;

import com.google.inject.Provider;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/**
 * Callable around an {@link Exporter}.
 */
public class CallableExporter implements Callable<ExportResult> {

  private Provider<Exporter> exporterProvider;
  private UUID jobId;
  private AuthData authData;
  private Optional<ExportInformation> exportInformation;

  public CallableExporter(Provider<Exporter> exporterProvider, UUID jobId, AuthData authData,
      Optional<ExportInformation> exportInformation) {
    this.exporterProvider = exporterProvider;

    this.jobId = jobId;
    this.authData = authData;
    this.exportInformation = exportInformation;
  }

  @Override
  public ExportResult call() throws Exception {
    return exporterProvider.get()
        .export(jobId, authData, exportInformation);
  }
}
