/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.constant;

/**
 * Defines error codes for Synology data transfer.
 *
 * <p>The codes are structured as follows:
 *
 * <ul>
 *   <li>S: Service identifier
 *   <li>MM: Module identifier
 *   <li>SS: Specific error code
 * </ul>
 *
 * <p>Example: 11001 represents a DTP Client error with the code MAX_RETRIES_EXCEEDED where 1 is the
 * service identifier, 10 is the module identifier, and 01 is the specific error code.
 */
public interface SynologyErrorCode {
  // [S][MM][SS]
  // S: Service identifier
  // MM: Module identifier
  // SS: Specific error code

  // Common Errors

  // DTP Client Errors
  public static final int MAX_RETRIES_EXCEEDED = 11001;
  public static final int IMPORT_FAILED = 11002;
  public static final int QUOTA_NOT_CLAIMED = 11003;
  public static final int DESTINATION_FULL = 11004;
}
