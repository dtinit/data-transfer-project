/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.cloud.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.Inject;
import org.dataportabilityproject.cloud.google.GoogleCloudModule.ProjectId;
import org.dataportabilityproject.cloud.interfaces.BucketStore;

final class GoogleBucketStore implements BucketStore {
  private static final String APP_CREDENTIAL_BUCKET_PREFIX = "app-data-";

  private Storage storage;
  private String bucketName;

  @Inject
  GoogleBucketStore(
      GoogleCredentials googleCredentials,
      @ProjectId String projectId) {
    storage = StorageOptions.newBuilder()
        .setProjectId(projectId)
        .setCredentials(googleCredentials)
        .build().getService();
    // Google Cloud Platform requires bucket names be unique across projects, so we include project
    // ID in the bucket name.
    bucketName = APP_CREDENTIAL_BUCKET_PREFIX + projectId;
  }

  public byte[] getAppCredentialBlob(String blobName) {
    Bucket bucket = storage.get(bucketName);
    return bucket.get(blobName).getContent();
  }
}
