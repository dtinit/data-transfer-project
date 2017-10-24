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
