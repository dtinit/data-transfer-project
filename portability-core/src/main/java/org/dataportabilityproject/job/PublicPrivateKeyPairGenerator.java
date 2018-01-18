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
package org.dataportabilityproject.job;

import com.google.common.io.BaseEncoding;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for generating, encoding, and decoding asymmetric keys.
 */
public class PublicPrivateKeyPairGenerator {
  private static final Logger logger = LoggerFactory.getLogger(PublicPrivateKeyPairGenerator.class);
  private static final String ALGORITHM = "RSA";

  /**
   * Generate a new KeyPair.
   */
  public static KeyPair generateKeyPair() {
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

  public static String encodeKey(Key key) {
    return BaseEncoding.base64Url().encode(key.getEncoded());
  }

  /**
   * Parses the given {@code encoded} public key.
   */
  public static PublicKey parsePublicKey(String encoded) {
    byte[] decoded = BaseEncoding.base64Url().decode(encoded);
    EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException for: {}", ALGORITHM, e);
      throw new RuntimeException("NoSuchAlgorithmException generating public keyFactory", e);
    }
    try {
      return keyFactory.generatePublic(spec);
    } catch (InvalidKeySpecException e) {
      logger.error("InvalidKeySpecException for: {}", spec.getEncoded().length, e);
      throw new RuntimeException("InvalidKeySpecException generating public key", e);
    }

  }

  /**
   * Parses the given {@code encoded} private key.
   */
  public static PrivateKey parsePrivateKey(String encoded) {
    byte[] decoded = BaseEncoding.base64Url().decode(encoded);
    EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException for: {}", ALGORITHM, e);
      throw new RuntimeException("NoSuchAlgorithmException generating private keyFactory", e);
    }
    try {
      return keyFactory.generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      logger.error("InvalidKeySpecException for: {}", spec.getEncoded().length, e);
      throw new RuntimeException("InvalidKeySpecException generating private key", e);
    }
  }
}
