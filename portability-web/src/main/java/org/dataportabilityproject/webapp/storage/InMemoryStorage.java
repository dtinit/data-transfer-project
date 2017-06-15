package org.dataportabilityproject.webapp.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory implementation of backend storage. */
public class InMemoryStorage implements Storage {
  private final ConcurrentHashMap<String, Map<String, String>> map;

  public InMemoryStorage() {
    map = new ConcurrentHashMap<>();
  }

  @Override
  public void put(String key, Map<String, String> data) {
    System.out.println("InMemoryStorage: put: key: " + key);
    map.put(key, data);
  }

  @Override
  public Map<String, String> get(String key) {
    System.out.println("InMemoryStorage: get: key: " + key);
    return map.get(key);
  }

  @Override
  public void delete(String key) {
    map.remove(key);
  }
}
