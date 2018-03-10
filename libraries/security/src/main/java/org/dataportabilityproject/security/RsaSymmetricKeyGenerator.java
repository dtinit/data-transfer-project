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
package org.dataportabilityproject.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSA-based implementation for {@link KeyPair} creation and encoding.
 *
 * TODO: provide a public module to bind KeyGenerators
 */
public class RsaSymmetricKeyGenerator implements AsymmetricKeyGenerator {

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

  @Override
  public PublicKey parse(byte[] encoded) {
    KeyFactory factory;
    try {
      factory = KeyFactory.getInstance(ALGORITHM);
      return factory.generatePublic(new X509EncodedKeySpec(encoded));
    } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
      logger.error("Error parsing public key for: {}", ALGORITHM, e);
      throw new RuntimeException("InvalidKeySpecException generating key", e);
    }
  }
}
