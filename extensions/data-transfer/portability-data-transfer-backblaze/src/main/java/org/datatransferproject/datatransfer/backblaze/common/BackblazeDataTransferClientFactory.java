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

package org.datatransferproject.datatransfer.backblaze.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class BackblazeDataTransferClientFactory {
  private final Map<UUID, BackblazeDataTransferClient> backblazeDataTransferClientMap;
  private final Monitor monitor;

  private static final long SIZE_THRESHOLD_FOR_MULTIPART_UPLOAD = 20 * 1024 * 1024; // 20 MB.
  private static final long PART_SIZE_FOR_MULTIPART_UPLOAD = 5 * 1024 * 1024; // 5 MB.

  public BackblazeDataTransferClientFactory(Monitor monitor) {
    this.monitor = monitor;
    this.backblazeDataTransferClientMap = new HashMap<>();
  }

  public BackblazeDataTransferClient getOrCreateB2Client( UUID jobId,
      TokenSecretAuthData authData)
      throws BackblazeCredentialsException, IOException {
    if (!backblazeDataTransferClientMap.containsKey(jobId)) {
      BackblazeDataTransferClient backblazeDataTransferClient =
              new BackblazeDataTransferClient(
                      monitor,
                      new BaseBackblazeS3ClientFactory(),
                      SIZE_THRESHOLD_FOR_MULTIPART_UPLOAD,
                      PART_SIZE_FOR_MULTIPART_UPLOAD);
      String exportService = JobMetadata.getExportService();
      backblazeDataTransferClient.init(authData.getToken(), authData.getSecret(), exportService);
      backblazeDataTransferClientMap.put(jobId, backblazeDataTransferClient);
    }
    return backblazeDataTransferClientMap.get(jobId);
  }
}
