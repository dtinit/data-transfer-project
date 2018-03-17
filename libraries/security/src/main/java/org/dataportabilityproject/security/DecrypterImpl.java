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

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides AES and RSA-based encryption implementations for decryption. See {@link
 * DecrypterFactory} to create.
 */
final class DecrypterImpl implements Decrypter {
  private static final Logger logger = LoggerFactory.getLogger(DecrypterImpl.class);

  private final Key key;
  private final String transformation;

  DecrypterImpl(String transformation, Key key) {
    this.key = key;
    this.transformation = transformation;
  }

  @Override
  public String decrypt(String encrypted) {
    try {
      byte[] decoded = BaseEncoding.base64Url().decode(encrypted);
      Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decrypted = cipher.doFinal(decoded);
      if (decrypted == null || decrypted.length <= 8) {
        throw new RuntimeException("incorrect decrypted text.");
      }
      byte[] data = new byte[decrypted.length - 8];
      System.arraycopy(decrypted, 8, data, 0, data.length);
      return new String(data, Charsets.UTF_8);
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      logger.error("Error decrypting data, length: {}", encrypted.length(), e);
      throw new RuntimeException(e);
    }
  }
}
