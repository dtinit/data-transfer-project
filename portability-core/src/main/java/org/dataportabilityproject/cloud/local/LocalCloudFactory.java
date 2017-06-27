package org.dataportabilityproject.cloud.local;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

/**
 * Factory that can create cloud interfaces when running locally.
 */
public class LocalCloudFactory implements CloudFactory {
  private static final Supplier<JobDataCache> JOB_DATA_CACHE_SUPPLIER =
      Suppliers.memoize(JobDataCacheImpl::new);

  private static final Supplier<PersistentKeyValueStore> KEY_VALUE_SUPPLIER =
      Suppliers.memoize(InMemoryPersistentKeyValueStore::new);

  @Override
  public synchronized JobDataCache getJobDataCache() {
    return JOB_DATA_CACHE_SUPPLIER.get();
  }

  @Override
  public PersistentKeyValueStore getPersistentKeyValueStore() {
    return KEY_VALUE_SUPPLIER.get();
  }
}
