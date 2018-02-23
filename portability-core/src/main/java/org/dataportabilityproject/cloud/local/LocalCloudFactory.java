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
package org.dataportabilityproject.cloud.local;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.dataportabilityproject.cloud.google.GoogleJobDataCache;
import org.dataportabilityproject.cloud.google.GoogleJobStore;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that can create cloud interfaces when running locally.
 */
@Singleton
public class LocalCloudFactory implements CloudFactory {
  private final Logger logger = LoggerFactory.getLogger(LocalCloudFactory.class);
  private static final String DUMMY_PROJECT_ID = "local-dev";
  private static final String USER_KEY_KIND = "local-user-key";
  private static final String JOB_KIND = "local-job";
  private static final LoadingCache<String, LoadingCache<String, JobDataCache>> JOB_DATA_CACHE =
      CacheBuilder.newBuilder()
      .build(new CacheLoader<String, LoadingCache<String, JobDataCache>>() {
        @Override
        public LoadingCache<String, JobDataCache> load(String id) throws Exception {
          return CacheBuilder.newBuilder()
              .build(new CacheLoader<String, JobDataCache>() {
                @Override
                public JobDataCache load(String service) throws Exception {
                  return new InMemoryJobDataCache();
                }
              });
        }
      });

  // Google DataStore emulator is used for local development
  private static final Datastore datastore = DatastoreOptions.newBuilder()
      .setHost("http://localhost:8081") // TODO: move to yaml config in case non-default is used
      .setProjectId(DUMMY_PROJECT_ID)
      .build()
      .getService();


  private final Supplier<JobStore> jobStoreSupplier;

  @Inject
  public LocalCloudFactory(CommonSettings commonSettings) {
    if (commonSettings.getEncryptedFlow()) {
      this.jobStoreSupplier =
          Suppliers.memoize( () ->
              new GoogleJobStore(datastore));
    } else {
      this.jobStoreSupplier =
          Suppliers.memoize( () -> new InMemoryKeyValueStore());
    }
  }

  @Override
  public JobDataCache getJobDataCache(UUID jobId, String service) {
    logger.info("Returning local google datastore-based cache");
    return new GoogleJobDataCache(datastore, jobId, service);
  }

  @Override
  public JobStore getJobStore() {
    JobStore store = jobStoreSupplier.get();
    logger.info("Returning local datastore-based key value store, {}", store.getClass().getName());
    return store;
  }

  @Override
  public CryptoKeyManagementSystem getCryptoKeyManagementSystem() {
    throw new UnsupportedOperationException("Local KMS not implemented yet");
  }

  @Override
  public BucketStore getBucketStore() {
    throw new UnsupportedOperationException("Local bucket store not implemented yet");
  }

  @Override
  public void clearJobData(UUID jobId) {
    QueryResults<Key> results = datastore.run(Query.newKeyQueryBuilder()
        .setKind(USER_KEY_KIND)
        .setFilter(PropertyFilter.hasAncestor(
            datastore.newKeyFactory().setKind(JOB_KIND).newKey(jobId.toString())))
        .build());
    results.forEachRemaining(datastore::delete);
  }
}
