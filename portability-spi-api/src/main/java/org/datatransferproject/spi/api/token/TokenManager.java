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
package org.datatransferproject.spi.api.token;

import java.util.UUID;

/** Functionality to manage the lifecycle of tokens. */
public interface TokenManager {

  /** Verifies if the token is valid. */
  boolean verifyToken(String token);

  /** Verifies and returns the jobId associated with this token. */
  UUID getJobIdFromToken(String token);

  /** Creates a new JWT token with the given {@code jobId}. */
  String createNewToken(UUID jobId);
}
