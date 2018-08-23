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
package org.datatransferproject.security.cleartext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.types.transfer.auth.AuthDataPair;

import java.io.IOException;
import java.security.PrivateKey;

/** */
public class ClearTextAuthDataDecryptService implements AuthDataDecryptService {
  private final ObjectMapper objectMapper;

  public ClearTextAuthDataDecryptService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean canHandle(String scheme) {
    return "cleartext".equals(scheme);
  }

  @Override
  public AuthDataPair decrypt(String encrypted, PrivateKey privateKey) {
    try {
      return objectMapper.readValue(encrypted, AuthDataPair.class);
    } catch (IOException e) {
      throw new SecurityException("Error deserializing auth tokens", e);
    }
  }
}
