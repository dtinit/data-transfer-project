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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

/**
 * Factory that can create cloud interfaces when running locally.
 */
public class LocalCloudFactory implements CloudFactory {
  private static final LoadingCache<String, LoadingCache<String, JobDataCache>> JOB_DATA_CACHE =
      CacheBuilder.newBuilder()
      .build(new CacheLoader<String, LoadingCache<String, JobDataCache>>() {
        @Override
        public LoadingCache<String, JobDataCache> load(String jobId) throws Exception {
          return CacheBuilder.newBuilder()
              .build(new CacheLoader<String, JobDataCache>() {
                @Override
                public JobDataCache load(String service) throws Exception {
                  return new JobDataCacheImpl();
                }
              });
        }
      });

  private static final Supplier<PersistentKeyValueStore> KEY_VALUE_SUPPLIER =
      Suppliers.memoize(InMemoryPersistentKeyValueStore::new);

  @Override
  public JobDataCache getJobDataCache(String jobId, String service) {
    try {
      return JOB_DATA_CACHE.get(jobId).get(service);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Couldn't creat loading jobdatacache", e);
    }
  }

  @Override
  public PersistentKeyValueStore getPersistentKeyValueStore() {
    return KEY_VALUE_SUPPLIER.get();
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
  public void clearJobData(String jobId) {
    JOB_DATA_CACHE.invalidate(jobId);
  }

  @Override
  public String getProjectId() {
    throw new UnsupportedOperationException(
        "Project ID not applicable when using the 'LOCAL' SupportedCloud");
  }
}
