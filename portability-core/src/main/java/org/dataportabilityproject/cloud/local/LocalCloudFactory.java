package org.dataportabilityproject.cloud.local;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
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
  public void clearJobData(String jobId) {
    JOB_DATA_CACHE.invalidate(jobId);
  }
}
