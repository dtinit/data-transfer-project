package org.dataportabilityproject.cloud.interfaces;

/**
 * Factory for creating object to interact with cloud implementations.
 */
public interface CloudFactory {
  JobDataCache getJobDataCache(String jobId, String service);
  PersistentKeyValueStore getPersistentKeyValueStore();
  void clearJobData(String jobId);
}
