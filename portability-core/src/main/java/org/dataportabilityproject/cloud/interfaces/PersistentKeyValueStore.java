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

  /**
   * Persist {@code data} with the given {@code key} overriding previous data, as part of the
   * current transaction.
   */
  void atomicPut(String key, Map<String, Object> data) throws IOException;

  /** Retrieve data with the given {@code key} or null if not found. */
  Map<String, Object> get(String key);

  /**
   * Retrieve data with the given {@code key} or null if not found. Do this as part of the
   * current transaction.
   */
  Map<String, Object> atomicGet(String key) throws IOException;

  /** Retrieve the first key that begins with the given {@code prefix} or null if none found. */
  String getFirst(String prefix);

  /** Deletes entry with the given {@code key}. */
  void delete(String key);

  /** Deletes entry with the given {@code key}, as part of the current transaction. */
  void atomicDelete(String key) throws IOException;

  /**
   * Start a transaction. All future atomic operations will use this transaction, until it is
   * committed or rolled back.
   */
  void startTransaction() throws IOException;

  /** Commit the current transaction. */
  void commitTransaction() throws IOException;

  /** Roll back the current transaction. */
  void rollbackTransaction() throws IOException;
}
