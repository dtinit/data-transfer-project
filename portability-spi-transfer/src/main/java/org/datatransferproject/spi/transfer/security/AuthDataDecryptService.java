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
package org.datatransferproject.spi.transfer.security;

import org.datatransferproject.types.transfer.auth.AuthDataPair;

import java.security.PrivateKey;

/** Decrypts authentication data for a given scheme. */
public interface AuthDataDecryptService {

  /** Returns true if this service supports the given scheme. */
  boolean canHandle(String scheme);

  /** Decrypts the data using the provided private key. */
  AuthDataPair decrypt(String encrypted, PrivateKey privateKey) throws SecurityException;
}
