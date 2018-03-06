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
package org.dataportabilityproject.security;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** AES-based implementation for {@link SecretKey} creation and encoding. */
class AesSymmetricKeyGenerator implements SymmetricKeyGenerator {

  private static final Logger logger = LoggerFactory.getLogger(AesSymmetricKeyGenerator.class);
  private static final String ALGORITHM = "AES";

  @Override
  public SecretKey generate() {
    try {
      KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
      return generator.generateKey();
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException for: {}", ALGORITHM, e);
      throw new RuntimeException("Error creating key generator", e);
    }
  }

  @Override
  public SecretKey parse(byte[] encoded) {
    return new SecretKeySpec(encoded, 0, encoded.length, ALGORITHM);
  }
}
