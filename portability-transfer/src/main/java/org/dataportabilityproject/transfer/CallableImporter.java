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
import java.util.UUID;
import java.util.concurrent.Callable;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

public class CallableImporter implements Callable<ImportResult> {

  private Provider<Importer> importerProvider;

  private UUID jobId;
  private AuthData authData;
  private DataModel data;

  public CallableImporter(Provider<Importer> importerProvider, UUID jobId, AuthData authData, DataModel data) {
    this.importerProvider = importerProvider;

    this.jobId = jobId;
    this.authData = authData;
    this.data = data;
  }

  @Override
  public ImportResult call() throws Exception {
    return importerProvider.get().importItem(jobId, authData, data);
  }
}
