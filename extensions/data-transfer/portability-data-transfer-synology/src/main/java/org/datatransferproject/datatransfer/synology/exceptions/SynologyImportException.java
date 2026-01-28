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

package org.datatransferproject.datatransfer.synology.exceptions;

import org.datatransferproject.datatransfer.synology.constant.SynologyErrorCode;

/** Exception thrown when an error occurs during a Synology import process. */
public class SynologyImportException extends SynologyDTPClientException {
  public SynologyImportException(String message) {
    super(message, "IMPORT_ERROR");
  }

  public SynologyImportException(String message, Throwable cause) {
    super(message, cause, String.valueOf(SynologyErrorCode.IMPORT_FAILED));
  }
}
