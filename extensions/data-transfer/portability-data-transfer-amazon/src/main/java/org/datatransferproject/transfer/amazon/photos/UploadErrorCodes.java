/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

/**
 * Client-facing Upload Service error codes we classify on. These mirror the external names in
 * the Upload Service's {@code UploadErrorCode} enum and are part of the API contract (changing
 * them would break backward compatibility), so matching on them is stable.
 */
final class UploadErrorCodes {

  private UploadErrorCodes() {}

  /** An identical item already exists (returned on HTTP 409). */
  static final String DUPLICATES_CONFLICT_ERROR = "DuplicatesConflictError";

  /** Destination storage quota exceeded (returned on HTTP 403, distinct from other 403s). */
  static final String INSUFFICIENT_STORAGE = "InsufficientStorage";

  /**
   * No active storage subscription for the account (returned on HTTP 403). Treated as a
   * storage-quota condition because the upload cannot proceed without sufficient allowance.
   */
  static final String NO_ACTIVE_SUBSCRIPTION_FOUND = "NoActiveSubscriptionFound";
}
