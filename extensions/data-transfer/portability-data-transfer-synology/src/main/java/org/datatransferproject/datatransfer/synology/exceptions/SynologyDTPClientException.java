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

/**
 * Base class for exceptions thrown by the Synology DTP client. Business logic exceptions should
 * extend this class.
 */
public abstract class SynologyDTPClientException extends SynologyException {
  protected SynologyDTPClientException(String message, String errorCode) {
    super(message, errorCode);
  }

  protected SynologyDTPClientException(String message, Throwable cause, String errorCode) {
    super(message, cause, errorCode);
  }
}
