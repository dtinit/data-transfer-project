package org.dataportabilityproject.cloud.interfaces;

/**
 * Factory for creating object to interact with cloud implementations.
 */
public interface CloudFactory {
  JobDataCache getJobDataCache();
  PersistentKeyValueStore getPersistentKeyValueStore();
}
