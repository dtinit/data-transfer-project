package org.dataportabilityproject.webapp.storage;

import java.util.Map;

/** Access to persistance layer */
public interface Storage {

  /** Persist {@code data} with the given {@code key} overriding previous data. */
  void put(String key, Map<String, Object> data);

  /** Retrieve data with the given {@code key} or null if not found. */
  Map<String, Object> get(String key);

  /** Deletes entry with the given {@code key}*/
  void delete(String key);
}
