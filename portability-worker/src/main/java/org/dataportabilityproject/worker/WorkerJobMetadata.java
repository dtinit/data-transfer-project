/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.worker;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.security.KeyPair;
import org.dataportabilityproject.job.PublicPrivateKeyPairGenerator;

/**
 * A class that contains the value of the job and key for a worker instance. This classes uses
 * the initialization-on-demand holder idiom to ensure it is a singleton.
 */
@Singleton
final class WorkerJobMetadata {
  private static KeyPair keyPair = null;
  private static String jobId = null;

  @Inject
  WorkerJobMetadata() {}

  boolean isInitialized() {
    return (jobId != null && keyPair != null);
  }

  void init(String jobId) {
    Preconditions.checkState(!isInitialized(), "WorkerJobMetadata cannot be initialized twice");
    this.jobId = jobId;
    this.keyPair = PublicPrivateKeyPairGenerator.generateKeyPair();
  }

  public KeyPair getKeyPair() {
    Preconditions.checkState(isInitialized(), "WorkerJobMetadata must be initialized");
    return keyPair;
  }

  public String getJobId() {
    Preconditions.checkState(isInitialized(), "WorkerJobMetadata must be initialized");
    return jobId;
  }
}
