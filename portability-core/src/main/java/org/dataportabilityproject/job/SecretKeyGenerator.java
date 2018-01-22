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
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates secret keys, e.g. for use to associate with a job and encrypt data between the client,
 * api, and worker.
 */
public class SecretKeyGenerator {
  private static final Logger logger = LoggerFactory.getLogger(SecretKeyGenerator.class);
  public static final String ALGORITHM = "AES";

  /** Generate a new symmetric key to use throughout the life of a job session.  */
  public static String generateKeyAndEncode() {
    try {
      KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
      SecretKey key = generator.generateKey();
      return BaseEncoding.base64Url().encode(key.getEncoded());
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException for: {}", ALGORITHM, e);
      throw new RuntimeException("Error creating key generator", e);
    }
  }

  public static SecretKey parse(String encoded) {
    byte[] decoded = BaseEncoding.base64Url().decode(encoded);
    return new SecretKeySpec(decoded, 0, decoded.length, SecretKeyGenerator.ALGORITHM);
  }
}
