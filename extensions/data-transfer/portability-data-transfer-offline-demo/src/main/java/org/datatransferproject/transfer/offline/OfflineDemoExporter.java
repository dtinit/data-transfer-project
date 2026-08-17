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
package org.datatransferproject.transfer.offline;

import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

/**
 * Simulates exporting offline data. For demo purposes only!
 *
 * <p>Returns a fixed payload without contacting any service, so a transfer can be run end to end
 * without provider credentials. The contents are deterministic so callers may assert on them.
 */
public class OfflineDemoExporter implements Exporter<TokenAuthData, DemoOfflineData> {

  /** The payload every export returns. */
  static final String CONTENTS = "offline-demo data";

  @Override
  public ExportResult<DemoOfflineData> export(
      UUID jobId, TokenAuthData authData, Optional<ExportInformation> exportInformation) {
    // Continuation data is left null: that, rather than the ResultType, is what stops
    // PortabilityInMemoryDataCopier#copyHelper from recursing.
    return new ExportResult<>(ExportResult.ResultType.END, new DemoOfflineData(CONTENTS));
  }
}
