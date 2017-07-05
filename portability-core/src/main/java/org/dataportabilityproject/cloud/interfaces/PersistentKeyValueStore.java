package org.dataportabilityproject.cloud.interfaces;

import java.io.IOException;
import java.util.Map;

/**
 * Stores data that is persisted indefinitely.
 */
// TODO(willard): Add TTLs to data
// TODO(willard): Change interface to take serializable data and not just Map<String, Object>,
//                I left it that way to make the refactor easier.
public interface PersistentKeyValueStore {

  /** Persist {@code data} with the given {@code key} overriding previous data. */
  void put(String key, Map<String, Object> data) throws IOException;

  /** Retrieve data with the given {@code key} or null if not found. */
  Map<String, Object> get(String key);

  /** Deletes entry with the given {@code key}*/
  void delete(String key);
}
