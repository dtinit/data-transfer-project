/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.photobucket.client.helper;

import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.transfer.photobucket.model.error.OverlimitException;
import org.datatransferproject.transfer.photobucket.model.error.PhotobucketException;

public class ExceptionTransformer {
  // Exception mapper, to transform PB exceptions to open source exceptions
  public static CopyExceptionWithFailureReason transformException(Exception e) {
    if (e instanceof OverlimitException)
      return new DestinationMemoryFullException(e.getMessage(), e);
    else return new PhotobucketException(e.getMessage(), e);
  }
}
