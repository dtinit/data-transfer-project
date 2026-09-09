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

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.types.common.models.DataVertical.OFFLINE_DATA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.junit.jupiter.api.Test;

public class OfflineDemoTransferTest {

  private static final UUID JOB_ID = UUID.randomUUID();
  private static final TokenAuthData AUTH_DATA = new TokenAuthData("123");

  @Test
  public void exportsFixedContents() {
    ExportResult<DemoOfflineData> result =
        new OfflineDemoExporter().export(JOB_ID, AUTH_DATA, Optional.empty());

    assertThat(result.getType()).isEqualTo(ExportResult.ResultType.END);
    assertThat(result.getExportedData().getContents()).isEqualTo(OfflineDemoExporter.CONTENTS);
  }

  @Test
  public void exportReturnsNoContinuationData() {
    // PortabilityInMemoryDataCopier#copyHelper recurses on continuation data, not on ResultType,
    // so null here is what actually ends the copy.
    ExportResult<DemoOfflineData> result =
        new OfflineDemoExporter().export(JOB_ID, AUTH_DATA, Optional.empty());

    assertThat(result.getContinuationData()).isNull();
  }

  @Test
  public void extensionSuppliesBothSidesOfOfflineData() {
    OfflineDemoTransferExtension extension = new OfflineDemoTransferExtension();

    assertThat(extension.getExporter(OFFLINE_DATA)).isInstanceOf(OfflineDemoExporter.class);
    assertThat(extension.getImporter(OFFLINE_DATA)).isInstanceOf(OfflineDemoImporter.class);
  }

  @Test
  public void extensionSuppliesNothingForOtherVerticals() {
    OfflineDemoTransferExtension extension = new OfflineDemoTransferExtension();

    assertThat(extension.getExporter(PHOTOS)).isNull();
    assertThat(extension.getImporter(PHOTOS)).isNull();
  }

  @Test
  public void importsExportedData() {
    ExportResult<DemoOfflineData> exported =
        new OfflineDemoExporter().export(JOB_ID, AUTH_DATA, Optional.empty());

    ImportResult result =
        new OfflineDemoImporter()
            .importItem(
                JOB_ID,
                mock(IdempotentImportExecutor.class),
                AUTH_DATA,
                exported.getExportedData());

    assertThat(result.getType()).isEqualTo(ImportResult.ResultType.OK);
  }
}
