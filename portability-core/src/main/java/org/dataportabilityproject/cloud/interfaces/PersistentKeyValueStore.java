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
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJob.JobState;

/**
 * Stores key-value data that is persisted indefinitely.
 */
// TODO(willard): Add TTLs to data
public interface PersistentKeyValueStore {

  /**
   * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code jobId}, or if there was a different
   * problem inserting the job.
   */
  void create(PortabilityJob job) throws IOException;

  /**
   * Finds the {@link PortabilityJob} keyed by {@code jobId} in the store, or null if none found.
   */
  PortabilityJob find(String jobId);

  /**
   * Finds the {@link PortabilityJob} keyed by {@code jobId} in the store, and verify it is in
   * state {@code jobState}.
   */
  PortabilityJob find(String jobId, JobState jobState);

  /**
   * Finds the ID of the first {@link PortabilityJob} in state {@code jobState} in the store, or nul
   * if none found.
   */
  String findFirst(JobState jobState);

  /**
   * Removes the {@link PortabilityJob} keyed by {@code jobId} in the store.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  void remove(String jobId) throws IOException;

  /**
   * Atomically updates the {@link PortabilityJob} keyed by {@code jobId} to {@code portabilityJob},
   * and verifies that it was previously in the expected {@code previousState}.
   *
   * @throws IOException if the job was not in the expected state in the store, or there was another
   * problem updating it.
   */
  void update(PortabilityJob job, JobState previousState) throws IOException;
}
