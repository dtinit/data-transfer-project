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
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import java.util.UUID;
import javax.inject.Inject;
import org.dataportabilityproject.cloud.google.GoogleCloudModule.ProjectId;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.spi.cloud.storage.JobStore;

public final class GoogleCloudFactory implements CloudFactory {
  private final Datastore datastore;
  private final JobStore jobStore;
  private final CryptoKeyManagementSystem cryptoKeyManagementSystem;
  private final BucketStore bucketStore;

  @Inject
  public GoogleCloudFactory(
      GoogleBucketStore googleBucketStore,
      GoogleCredentials googleCredentials,
      GoogleCryptoKeyManagementSystem googleCryptoKeyManagementSystem,
      @ProjectId String projectId) {
    this.datastore = DatastoreOptions
        .newBuilder()
        .setProjectId(projectId)
        .setCredentials(googleCredentials)
        .build()
        .getService();
    this.jobStore = new GoogleJobStore(datastore);
    this.cryptoKeyManagementSystem = googleCryptoKeyManagementSystem;
    this.bucketStore = googleBucketStore;
  }

  @Override
  public JobDataCache getJobDataCache(UUID jobId, String service) {
    return new GoogleJobDataCache(datastore, jobId, service);
  }

  @Override
  public JobStore getJobStore() {
    return jobStore;
  }

  @Override
  public CryptoKeyManagementSystem getCryptoKeyManagementSystem() {
    return cryptoKeyManagementSystem;
  }

  @Override
  public BucketStore getBucketStore() {
    return bucketStore;
  }

  @Override
  public void clearJobData(UUID jobId) {
    QueryResults<Key> results = datastore.run(Query.newKeyQueryBuilder()
        .setKind(GoogleJobDataCache.USER_KEY_KIND)
        .setFilter(PropertyFilter.hasAncestor(datastore.newKeyFactory()
            .setKind(GoogleJobDataCache.JOB_KIND).newKey(jobId.toString()))).build());
    results.forEachRemaining(datastore::delete);
  }
}
