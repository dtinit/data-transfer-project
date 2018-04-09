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
package org.dataportabilityproject.api.action.startjob;

import java.util.UUID;

/** A request to start a job for the given {@code jobId} and credentials. */
public class StartJobActionRequest {
  private final UUID jobId;
  /** Serialiazed auth credential encrypted with the symmetric session key for this job. */
  private final String encryptedExportAuthCredential;
  /** Serialiazed auth credential encrypted with the symmetric session key for this job. */
  private final String encryptedImportAuthCredential;

  public StartJobActionRequest(
      UUID jobId, String encryptedExportAuthCredential, String encryptedImportAuthCredential) {
    this.jobId = jobId;
    this.encryptedExportAuthCredential = encryptedExportAuthCredential;
    this.encryptedImportAuthCredential = encryptedImportAuthCredential;
  }

  public UUID getJobId() {
    return jobId;
  }

  public String getEncryptedExportAuthCredential() {
    return encryptedExportAuthCredential;
  }

  public String getEncryptedImportAuthCredential() {
    return encryptedImportAuthCredential;
  }
}
