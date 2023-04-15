/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.constants;

import org.jetbrains.annotations.NotNull;

/** Constants that are specific to Photos & Videos. */
public class ApplePhotosConstants {
  // Content transfer encoding chunk length, default 50MB
  public static final Integer contentRequestLength = 50_000_000;

  // Maximum num of albums import in a single request
  public static final Integer maxNewAlbumRequests = 50;

  // Maximum num of media import in a single request
  public static final Integer maxNewMediaRequests = 50;
  // maximum media size that can be transferred, default 50GB
  public static final Long maxMediaTransferSize = 50_000_000_000L;
  public static final String BYTES_KEY = "bytes";
  public static final String COUNT_KEY = "count";

  public enum AppleMediaType {
    IMAGE("image"),
    VIDEO("video"),
    MEDIA("media");

    private final String val;

    AppleMediaType(final String val) {
      this.val = val;
    }

    @NotNull
    public String toString() {
      return this.val;
    }
  }
}
