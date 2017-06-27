package org.dataportabilityproject.cloud.local;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

/** In-memory implementation of backend storage. */
final class InMemoryPersistentKeyValueStore implements PersistentKeyValueStore {
  private final ConcurrentHashMap<String, Map<String, Object>> map;

  public InMemoryPersistentKeyValueStore() {
    map = new ConcurrentHashMap<>();
  }

  @Override
  public void put(String key, Map<String, Object> data) {
    System.out.println("InMemoryStorage: put: key: " + key);
    map.put(key, data);
  }

  @Override
  public Map<String, Object> get(String key) {
    System.out.println("InMemoryStorage: get: key: " + key);
    return map.get(key);
  }

  @Override
  public void delete(String key) {
    map.remove(key);
  }
}
