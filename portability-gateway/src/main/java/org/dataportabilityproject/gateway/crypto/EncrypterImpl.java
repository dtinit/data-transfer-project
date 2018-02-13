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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides AES and RSA-based encryption implementations. See {@link EncrypterFactory} to create.
 */
final class EncrypterImpl implements Encrypter {

  private static final Logger logger = LoggerFactory.getLogger(EncrypterImpl.class);
  private static final String SYMMETRIC_ALGORITHM = "AES";

  private final Key key;
  private final String transformation;

  private EncrypterImpl(String transformation, Key key) {
    this.key = key;
    this.transformation = transformation;
  }

  @Override
  public String encrypt(String data) {
    try {
      Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] salt = new byte[8];
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      random.nextBytes(salt);
      cipher.update(salt);
      byte[] encrypted = cipher.doFinal(data.getBytes(Charsets.UTF_8));
      return BaseEncoding.base64Url().encode(encrypted);
    } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException
        | NoSuchAlgorithmException | NoSuchPaddingException e) {
      logger.error("Exception encrypting data, length: {}", data.length(), e);
      throw new RuntimeException(e);
    }
  }

  public static class EncrypterFactory {

    /**
     * Creates a {@link EncrypterImpl} with the given {@link SecretKey} for use with "AES"-based
     * symmetric encryption.
     */
    public static Encrypter create(SecretKey key) {
      Preconditions.checkArgument(key.getAlgorithm().equals("AES"));
      return new EncrypterImpl("AES", key);
    }

    /**
     * Creates a {@link EncrypterImpl} with the given {@link PublicKey} for use with "RSA"-based
     * asymmetric encryption.
     */
    public static Encrypter create(PublicKey key) {
      Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
      return new EncrypterImpl("RSA/ECB/PKCS1Padding", key);
    }

    /**
     * Creates a {@link EncrypterImpl} with the given {@link PrivateKey} for use with "RSA"-based
     * asymmetric encryption.
     */
    public static Encrypter create(PrivateKey key) {
      Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
      return new EncrypterImpl("RSA/ECB/PKCS1Padding", key);
    }
  }
}
