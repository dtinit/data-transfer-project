/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.transfer.microsoft.transformer.common;

import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Utility methods for transforming data. */
public final class TransformerHelper {

  private TransformerHelper() {}

  public static Optional<String> getString(String key, Map<String, ?> map) {
    return Optional.ofNullable((String) map.get(key));
  }

  @SuppressWarnings("unchecked")
  public static Optional<Map<String, String>> getMap(String key, Map<String, ?> map) {
    return Optional.ofNullable((Map<String, String>) map.get(key));
  }

  public static String getOrDefault(
      String firstKey, String secondKey, Map<String, ?> map, String defaultValue) {
    return getMap(firstKey, map).orElse(emptyMap()).getOrDefault(secondKey, defaultValue);
  }

  @SuppressWarnings("unchecked")
  public static Optional<List<String>> getList(String key, Map<String, ?> map) {
    return Optional.ofNullable((List<String>) map.get(key));
  }

  @SuppressWarnings("unchecked")
  public static Optional<List<Map<String, String>>> getListMap(String key, Map<String, ?> map) {
    return Optional.ofNullable((List<Map<String, String>>) map.get(key));
  }

  public static <K, V> void safeSet(K key, V value, Map<K, V> map) {
    if (value == null) {
      return;
    }
    map.put(key, value);
  }
}
