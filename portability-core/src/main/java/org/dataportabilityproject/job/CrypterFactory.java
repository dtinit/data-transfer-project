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
package org.dataportabilityproject.job;

import com.google.common.base.Preconditions;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

/**
 * Constructs instances of the {@link Crypter} for different key types. See
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html
 */
public class CrypterFactory {

  /**
   * Creates a {@link Crypter} with the given {@link SecretKey} for use with "AES"-based symmetric
   * encryption.
   */
  public static Crypter create(SecretKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("AES"));
    return new CrypterImpl("AES", key);
  }

  /**
   * Creates a {@link Crypter} with the given {@link PublicKey} for use with "RSA"-based asymmetric
   * encryption.
   */
  public static Crypter create(PublicKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new CrypterImpl("RSA/ECB/PKCS1Padding", key);
  }

  /**
   * Creates a {@link Crypter} with the given {@link PrivateKey} for use with "RSA"-based asymmetric
   * encryption.
   */
  public static Crypter create(PrivateKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new CrypterImpl("RSA/ECB/PKCS1Padding", key);
  }
}
