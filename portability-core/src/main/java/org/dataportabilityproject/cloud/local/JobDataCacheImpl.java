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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;

final class JobDataCacheImpl implements JobDataCache {
  private static HashMap<String, byte[]> map = new HashMap<>();

  @Override
  public boolean hasKey(String key) {
    return map.containsKey(key);
  }

  @Override
  public <T extends Serializable> T getData(String key, Class<T> clazz) throws IOException {
    if (!map.containsKey(key)) {
      throw new IllegalArgumentException("Can't get key: " + key);
    }

    ByteArrayInputStream bis = new ByteArrayInputStream(map.get(key));
    ObjectInputStream in = new ObjectInputStream(bis);
    try {
      return (T) in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("problem deserilizing object", e);
    }
  }

  @Override
  public <T extends Serializable> void store(String key, T data) throws IOException {
    if (map.containsKey(key)) {
      throw new IllegalArgumentException("Can't restore key: " + key);
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(data);
    }
    map.put(key, bos.toByteArray());
  }
}
