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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * A class that contains the value of the job and key for a worker instance. This classes uses
 * the initialization-on-demand holder idiom to ensure it is a singleton.
 */
class WorkerJobMetadata {
  private static KeyPair keyPair = null;
  private static String jobId = null;

  private WorkerJobMetadata() {}

  boolean isInitialized() {
    return (jobId != null && keyPair != null);
  }

  void init(String jobId) {
    Preconditions.checkState(!isInitialized(), "WorkerJobMetadata cannot be initialized twice");
    this.jobId = jobId;
    this.keyPair = createKey();
  }

  public KeyPair getKeyPair() {
    Preconditions.checkState(isInitialized(), "WorkerJobMetadata must be initialized");
    return keyPair;
  }

  public String getJobId() {
    Preconditions.checkState(isInitialized(), "WorkerJobMetadata must be initialized");
    return jobId;
  }

  private static class WorkerJobMetadataHolder {

    private static WorkerJobMetadata INSTANCE = new WorkerJobMetadata();
  }

  /**
   * Creates a Singleton instance of WorkerJobMetadata.
   */
  public static WorkerJobMetadata getInstance() {
    return WorkerJobMetadataHolder.INSTANCE;
  }

  /**
   * Creates the public/private instance key pair.
   */
  private KeyPair createKey() {
    // TODO: Implement
    return new KeyPair(new PublicKey() {
      @Override
      public String getAlgorithm() {
        return null;
      }

      @Override
      public String getFormat() {
        return null;
      }

      @Override
      public byte[] getEncoded() {
        return new byte[0];
      }
    }, new PrivateKey() {
      @Override
      public String getAlgorithm() {
        return null;
      }

      @Override
      public String getFormat() {
        return null;
      }

      @Override
      public byte[] getEncoded() {
        return new byte[0];
      }
    }); // TODO: Implement
  }
}
