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

package org.datatransferproject.cloud.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.InputStream;
import java.util.UUID;

/**
 * Class for temporarily storing user data for transfer
 */
public class GoogleUserDataStore {
  private final Bucket bucket;

  @Inject
  public GoogleUserDataStore(Bucket bucket) {
    this.bucket = bucket;
  }

  public Blob create(UUID jobId, String keyName, InputStream inputStream) {
    String blobName = getDataKeyName(jobId, keyName);
    return bucket.create(blobName, inputStream);
  }

  public Blob get(UUID jobId, String keyName) {
    String blobName = getDataKeyName(jobId, keyName);
    return bucket.get(blobName);
  }

  @VisibleForTesting
  static String getDataKeyName(UUID jobId, String key) {
    return String.format("%s-%s", jobId, key);
  }
}
