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
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.job.PortabilityJob;

/**
 * Stores data that is persisted indefinitely.
 */
// TODO(willard): Add TTLs to data
// TODO(willard): Change interface to take serializable data and not just Map<String, Object>,
//                I left it that way to make the refactor easier.
public interface PersistentKeyValueStore {

  /**
   *  Puts {@code job} in the database for the first time, keyed by {@code jobId}, and verifies
   *  it doesn't already exist. To update the value for an already existing {@code jobId},
   *  use atomicUpdate instead.
   *
   *  @throws IOException if an entry already exists for {@code jobId}.
   */
  // TODO make this take PortabilityJob directly
  void put(String jobId, PortabilityJob job) throws IOException;

  /** Retrieve data with the given {@code jobId} or null if not found. */
  PortabilityJob get(String jobId);

  /** Retrieve the ID of the first job in the given job state, or null if none found. */
  String getFirst(JobState jobState);

  /** Deletes job with the given {@code JobId}.
   *
   * @throws IOException if the jobID didn't exist or deletion was unsuccessful for another reason.
   */
  void delete(String jobId) throws IOException;

  /**
   * Atomically updates {@code jobId} to {@code job} and verifies it was previously in state
   * {@code previousState}.
   *
   * @throws IOException if the jobID didn't exist in the expected state or the update was
   * unsuccessful for another reason.
   **/
  void atomicUpdate(String jobId, JobState previousState, PortabilityJob job) throws IOException;
}
