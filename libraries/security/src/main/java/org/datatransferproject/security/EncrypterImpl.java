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

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.datatransferproject.api.launcher.Monitor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static java.lang.String.format;

/**
 * Provides AES and RSA-based encryption implementations. See {@link EncrypterFactory} to create.
 */
final class EncrypterImpl implements Encrypter {
  private final Key key;
  private final String transformation;
  private final Monitor monitor;

  EncrypterImpl(String transformation, Key key, Monitor monitor) {
    this.key = key;
    this.transformation = transformation;
    this.monitor = monitor;
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
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      monitor.severe(() -> format("Exception encrypting data, length: %s", data.length()), e);
      throw new RuntimeException(e);
    }
  }
}
