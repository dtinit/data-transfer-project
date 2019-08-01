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
package org.datatransferproject.spi.transfer.security;

/** Creates an instance id and asymmetric encryption key pair unique to each worker instance. */
public interface TransferKeyGenerator {

  interface WorkerKeyPair {
    /** A unique id for the worker instance associated with this pair of asymmetric keys. */
    String getInstanceId();
    /** The encoded public key to encrypt data sent to this worker instance. */
    byte[] getEncodedPublicKey();
    /** The encoded private key to decrypt data sent to this worker instance. */
    byte[] getEncodedPrivateKey();
  }

  /**
   * Generates a {@link WorkerKeyPair} representing encoded private key and associated privatekey
   */
  WorkerKeyPair generate();
}
