/*
 * Copyright 2018 The Data-Portability Project Authors.
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

import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.spi.types.MicrosoftOfflineData;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

import java.util.UUID;

/**
 * Simulates importing offline data. For demo purposes only!
 *
 * <p>Microsoft offline data is used since that is the only form currently supported.
 */
public class OfflineDemoImporter implements Importer<TokenAuthData, MicrosoftOfflineData> {

  @Override
  public ImportResult importItem(UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenAuthData authData,
      MicrosoftOfflineData data) {
    // Print to the console to simulate an import
    System.out.println("Received offline data:\n" + data.getContents());
    return ImportResult.OK;
  }
}
