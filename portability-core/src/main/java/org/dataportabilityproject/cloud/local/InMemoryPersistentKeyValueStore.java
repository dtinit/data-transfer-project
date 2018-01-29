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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.cloud.google.GooglePersistentKeyValueStore;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory implementation of backend storage.
 */
public final class InMemoryPersistentKeyValueStore implements PersistentKeyValueStore {
  private static final Logger logger =
      LoggerFactory.getLogger(InMemoryPersistentKeyValueStore.class);

  private final ConcurrentHashMap<String, Map<String, Object>> map;

  public InMemoryPersistentKeyValueStore() {
    map = new ConcurrentHashMap<>();
  }

  @Override
  public void put(String key, Map<String, Object> data) {
    map.put(key, data);
  }

  @Override
  public Map<String, Object> get(String key) {
    return map.get(key);
  }

  @Override
  public String getFirst(String prefix) {
    // Mimic an index lookup
    for (String key : map.keySet()) {
      if (key.startsWith(prefix)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public void delete(String key) {
    map.remove(key);
  }

  /**
   * Update map, but note that this is not atomic or threadsafe currently.
   *
   * TODO: Implement threadsafe, atomic version if necessary. This class is only currently used for
   * local development, where we typically don't test concurrent requests.
   */
  @Override
  public boolean atomicUpdate(String previousKeyStr, String newKeyStr, Map<String, Object> data) {
    Map<String, Object> previousData = get(previousKeyStr);
    if (previousData == null) {
      logger.debug("Could not find previous key {}", previousKeyStr);
      return false;
    }

    Map<String, Object> newData = get(newKeyStr);
    if (newData != null) {
      logger.debug("Updated key already exists: {}", newKeyStr);
    }

    put(newKeyStr, data);
    delete(previousKeyStr);
    return true;
  }

}
