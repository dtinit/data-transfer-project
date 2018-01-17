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
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for generating, encoding, and decoding asymmetric keys.
 */
public class PublicPrivateKeyUtils {
  private static final Logger logger = LoggerFactory.getLogger(PublicPrivateKeyUtils.class);

  /**
   * Generate a new KeyPair.
   */
  public static KeyPair generateKeyPair() {
    KeyPairGenerator kpg = null;
    try {
      kpg = KeyPairGenerator.getInstance("DSA", "SUN");
    } catch (NoSuchProviderException e) {
      throw new RuntimeException("NoSuchProviderException generating key", e);
    } catch (NoSuchAlgorithmException e) {
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
    logger.debug("decoded length: {}", decoded.length);
    EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decoded);
    logger.debug("pubKeySpec generated for: {}", decoded.length);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance("DSA", "SUN");
      logger.debug("public keyFactory generated for: {}", "DSA - SUN");
    } catch (NoSuchAlgorithmException e) {
      logger.debug("NoSuchAlgorithmException for: {}", "DSA - SUN");
      e.printStackTrace();
      throw new RuntimeException("NoSuchAlgorithmException generating public keyFactory", e);
    } catch (Exception e) {
      logger.debug("Exception for: {}", "DSA - SUN");
      e.printStackTrace();
      throw new RuntimeException("Exception generating  public keyFactory", e);
    }
    try {
      logger.debug("About to generate PublicKey");
      return keyFactory.generatePublic(pubKeySpec);
    } catch (InvalidKeySpecException e) {
      logger.debug("InvalidKeySpecException for: {}", pubKeySpec);
      e.printStackTrace();
      throw new RuntimeException("InvalidKeySpecException generating public key", e);
    } catch (Exception e) {
      logger.debug("Exception for: {}", "DSA - SUN");
      e.printStackTrace();
      throw new RuntimeException("Exception generating public key", e);
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
      keyFactory = KeyFactory.getInstance("DSA", "SUN");
    } catch (NoSuchProviderException e) {
      throw new RuntimeException("NoSuchProviderException generating private keyFactory", e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("NoSuchAlgorithmException generating private keyFactory", e);
    }
    try {
      return keyFactory.generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException("InvalidKeySpecException generating private key", e);
    }
  }
}
