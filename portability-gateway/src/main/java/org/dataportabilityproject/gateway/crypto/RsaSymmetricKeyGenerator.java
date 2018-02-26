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
package org.dataportabilityproject.gateway.crypto;

import com.google.common.io.BaseEncoding;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSA-based implementation for {@link KeyPair} creation and encoding.
 */
class RsaSymmetricKeyGenerator implements AsymmetricKeyGenerator {

  private static final Logger logger = LoggerFactory.getLogger(RsaSymmetricKeyGenerator.class);
  private static final String ALGORITHM = "RSA";

  @Override
  public KeyPair generate() {
    KeyPairGenerator kpg = null;
    try {
      kpg = KeyPairGenerator.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException for: {}", ALGORITHM, e);
      throw new RuntimeException("NoSuchAlgorithmException generating key", e);
    }
    kpg.initialize(1024);
    return kpg.genKeyPair();
  }
}
