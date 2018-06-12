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
package org.dataportabilityproject.transfer.derived;

import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.microsoft.spi.types.MicrosoftDerivedData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;

import java.util.UUID;

/** Simulates importing derived data. For demo purposes only! */
public class DerivedDemoImporter implements Importer<TokenAuthData, MicrosoftDerivedData> {

  @Override
  public ImportResult importItem(UUID jobId, TokenAuthData authData, MicrosoftDerivedData data) {
    // Print to the console to simulate an import
    System.out.println("Received derived data:\n" + data.getContents());
    return ImportResult.OK;
  }
}
