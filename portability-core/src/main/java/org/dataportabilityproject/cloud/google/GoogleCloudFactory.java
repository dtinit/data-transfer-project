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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.inject.Inject;
import org.dataportabilityproject.cloud.google.Annotations.ProjectId;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoogleCloudFactory implements CloudFactory {
  private final Logger logger = LoggerFactory.getLogger(GoogleCloudFactory.class);
  // Lazy init this in case we are running a different cloud than Google, in which case this class
  // won't be used and the environment variable this is set from won't be available.
  private static String PROJECT_ID;

  private final Datastore datastore;
  private final PersistentKeyValueStore persistentKeyValueStore;
  private final CryptoKeyManagementSystem cryptoKeyManagementSystem;
  private final BucketStore bucketStore;
  private final String projectId;

  @Inject
  public GoogleCloudFactory(
      GoogleBucketStore googleBucketStore,
      GoogleCredentials googleCredentials,
      GoogleCryptoKeyManagementSystem googleCryptoKeyManagementSystem,
      @ProjectId String projectId) {
    logger.warn("\n\n\nGoogleCloudFactory being constructed!\n\n\n");
    this.datastore = DatastoreOptions
        .newBuilder()
        .setProjectId(projectId)
        .setCredentials(googleCredentials)
        .build()
        .getService();
    this.persistentKeyValueStore = new GooglePersistentKeyValueStore(datastore);
    this.cryptoKeyManagementSystem = googleCryptoKeyManagementSystem;
    this.bucketStore = googleBucketStore;
    this.projectId = projectId;
  }

  @Override
  public JobDataCache getJobDataCache(String jobId, String service) {
    return new GoogleJobDataCache(datastore, jobId, service);
  }

  @Override
  public PersistentKeyValueStore getPersistentKeyValueStore() {
    return persistentKeyValueStore;
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
  public void clearJobData(String jobId) {
    QueryResults<Key> results = datastore.run(Query.newKeyQueryBuilder()
        .setKind(GoogleJobDataCache.USER_KEY_KIND)
        .setFilter(PropertyFilter.hasAncestor(
            datastore.newKeyFactory().setKind(GoogleJobDataCache.JOB_KIND).newKey(jobId)))
        .build());
    results.forEachRemaining(datastore::delete);
  }

  /**
   * Google's implementation of project ID to use in generic (non-Google-specific) code like
   * {@code AppCredentials}.
   */
  @Override
  public String getProjectId() {
    return projectId;
  }
}
