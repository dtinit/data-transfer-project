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
package org.datatransferproject.security;

import org.datatransferproject.api.launcher.Monitor;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

/** AES-based implementation for {@link SecretKey} creation and encoding. */
public class AesSymmetricKeyGenerator implements SymmetricKeyGenerator {
  // TODO change this back to package private when Guice module is created
  private static final String ALGORITHM = "AES";

  private final Monitor monitor;

  public AesSymmetricKeyGenerator(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public SecretKey generate() {
    try {
      KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
      return generator.generateKey();
    } catch (NoSuchAlgorithmException e) {
      monitor.severe(() -> "NoSuchAlgorithmException for: " + ALGORITHM, e);
      throw new RuntimeException("Error creating key generator", e);
    }
  }

  @Override
  public SecretKey parse(byte[] encoded) {
    return new SecretKeySpec(encoded, 0, encoded.length, ALGORITHM);
  }
}
