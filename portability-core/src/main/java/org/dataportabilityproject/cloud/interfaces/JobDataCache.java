package org.dataportabilityproject.cloud.interfaces;

import java.io.IOException;
import java.io.Serializable;

/**
 * A cache that is specific per service and per job.  It is unmodifiable.
 */
public interface JobDataCache {
  boolean hasKey(String key) throws IOException;
  <T extends Serializable> T getData(String key, Class<T> clazz) throws IOException;
  <T extends Serializable> void store(String key, T data) throws IOException;
}
