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
package org.datatransferproject.security.cleartext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.security.TransferKeyGenerator;

/**
 * RSA-based implementation for {@link KeyPair} creation. This will go unused in the clear text
 * flow. TODO: Move to a common location and reconcile with JWE RsaSymmetricKeyGenerator
 */
public class ClearTextKeyGenerator implements TransferKeyGenerator {

  private static final String ALGORITHM = "RSA";
  private final Monitor monitor;

  public ClearTextKeyGenerator(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public WorkerKeyPair generate() {
    KeyPairGenerator kpg = null;
    try {
      kpg = KeyPairGenerator.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      monitor.severe(() -> "NoSuchAlgorithmException for: " + ALGORITHM, e);
      throw new RuntimeException("NoSuchAlgorithmException generating key", e);
    }
    kpg.initialize(1024);
    KeyPair keyPair = kpg.genKeyPair();
    return new WorkerKeyPair() {
      @Override
      public byte[] getEncodedPublicKey() {
        return keyPair.getPublic().getEncoded();
      }

      @Override
      public byte[] getEncodedPrivateKey() {
        return keyPair.getPrivate().getEncoded();
      }
    };
  }
}
