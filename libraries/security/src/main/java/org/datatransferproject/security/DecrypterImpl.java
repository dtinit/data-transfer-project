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

import static java.lang.String.format;

/**
 * Provides AES and RSA-based encryption implementations for decryption. See {@link
 * DecrypterFactory} to create.
 */
final class DecrypterImpl implements Decrypter {
  private final Key key;
  private final String transformation;
  private final Monitor monitor;

  DecrypterImpl(String transformation, Key key, Monitor monitor) {
    this.key = key;
    this.transformation = transformation;
    this.monitor = monitor;
  }

  @Override
  public String decrypt(String encrypted) {
    try {
      byte[] decoded = BaseEncoding.base64Url().decode(encrypted);
      Cipher cipher;
      // This odd switch statement ensures we're using a compile-time constant
      // to define our cipher transformation. This prevents issues with certain
      // compilers.
      switch (transformation) {
      case CryptoTransformations.AES_CBC_NOPADDING:
        cipher = Cipher.getInstance(CryptoTransformations.AES_CBC_NOPADDING);
        break;
      case CryptoTransformations.RSA_ECB_PKCS1:
        cipher = Cipher.getInstance(CryptoTransformations.RSA_ECB_PKCS1);
        break;
      default:
        throw new RuntimeException(
          format(
            "Invalid cipher transformation, got: %s, expected one of:[%s, %s]",
            transformation,
            CryptoTransformations.AES_CBC_NOPADDING,
            CryptoTransformations.RSA_ECB_PKCS1));
      }
      // don't submit before checking init logic
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
      monitor.severe(() -> format("Error decrypting data, length: %s", encrypted.length()), e);
      throw new RuntimeException(e);
    }
  }
}
