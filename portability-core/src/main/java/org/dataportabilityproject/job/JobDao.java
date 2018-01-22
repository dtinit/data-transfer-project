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
package org.dataportabilityproject.job;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data acccess object that provides functionality to manage persisted data for portability jobs.
 */
public class JobDao {
  // Keys for specific values in data store
  private static final String ID_DATA_KEY = "UUID";
  private static final String KEY_SEPERATOR = "::";
  private static final Logger logger = LoggerFactory.getLogger(JobDao.class);

  /**
   * The current state of the job.
   * <p>
   * The value PENDING_WORKER_ASSIGNMENT indicates the client has sent a request for a worker to be assigned before
   * sending all the data required for the job.
   * The value ASSIGNED_WITHOUT_AUTH_DATA indicates the client has submitted all data required, such as the
   * encrypted auth data, in order to begin processing the portability job.
   */
  @VisibleForTesting // Package-Private
  public enum JobState {
    PENDING_AUTH_DATA, // The job has not finished the authorization flows
    PENDING_WORKER_ASSIGNMENT, // The job has all authorization information but is not assigned a worker yet
    ASSIGNED_WITHOUT_AUTH_DATA, // The job is assigned a worker and waiting for auth data from the api
    ASSIGNED_WITH_AUTH_DATA, // The job is assigned a worker and has encrypted auth data
  }

  private final PersistentKeyValueStore storage;

  @Inject
  public JobDao(CloudFactory cloudFactory) {
    this.storage = cloudFactory.getPersistentKeyValueStore();
  }

  @VisibleForTesting
  public JobDao(PersistentKeyValueStore persistentKeyValueStore) {
    this.storage = persistentKeyValueStore;
  }

  // INDEX LOOKUP METHODS

  /** Returns the next job in unassigned state. */
  public String findNextJobPendingWorkerAssignment() {
    String key = storage.getFirst(JobState.PENDING_WORKER_ASSIGNMENT.name());
    if (key != null) {
      return getIdFromKey(key);
    }
    return null;
  }


  // LOOKUP METHODS

  /** Looks up a job that is not yet completely populated with auth data. */
  public PortabilityJob lookupJobPendingAuthData(String id) {
    return lookupJob(id, JobState.PENDING_AUTH_DATA);
  }

  /** Looks up a job that is pending worker assignment. */
  public PortabilityJob lookupJobPendingWorkerAssignment(String id) {
    return lookupJob(id, JobState.PENDING_WORKER_ASSIGNMENT);
  }

  /** Looks up a job that is assigned to a worker and is ready to be processed. */
  public PortabilityJob lookupAssignedWithoutAuthDataJob(String id) {
    return lookupJob(id, JobState.ASSIGNED_WITHOUT_AUTH_DATA);
  }

  // TODO: Only expose for testing
  /** Looks up a job that is assigned to a worker and is ready to be processed. */
  public PortabilityJob lookupAssignedWithAuthDataJob(String id) {
    return lookupJob(id, JobState.ASSIGNED_WITH_AUTH_DATA);
  }

  private PortabilityJob lookupJob(String id, JobState jobState) {
    Preconditions.checkNotNull(id);
    Map<String, Object> data = storage.get(createKey(jobState, id));
    if (data == null || data.isEmpty()) {
      return null;
    }
    PortabilityJob job = PortabilityJob.mapToJob(data);
    return job;
  }


  // INSERT METHODS

  /**
   * Inserts a new entry for the given {@code id} in storage in incomplete state.
   */
  public void insertJobInPendingAuthDataState(PortabilityJob job) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(job.id()));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(job.dataType()));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(job.exportService()));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(job.importService()));
    for (JobState state : JobState.values()) {
      String key = createKey(state, job.id());
      Preconditions.checkArgument((storage.get(key) == null),
          "Attempting to insert an already existing job");
    }

    String key = createKey(JobState.PENDING_AUTH_DATA, job.id());
    storage.put(key, job.asMap());
  }

  // UPDATE METHODS

  /**
   * Updates an existing incomplete {@code job} in storage.
   */
  public void updatePendingAuthDataJob(PortabilityJob job) throws IOException {
    updateJob(job, JobState.PENDING_AUTH_DATA);
  }

  /**
   * Replaces the job in PENDING_AUTH_DATA state to PENDING_WORKER_ASSIGNMENT state with
   * encryption data included.
   */
  public void updateJobStateToPendingWorkerAssignment(String id) throws IOException {
    // Verify job is in existing state
    PortabilityJob existingJob = lookupJob(id, JobState.PENDING_AUTH_DATA);
    // No updated to the job itself, just the state
    updateJobState(existingJob, JobState.PENDING_AUTH_DATA, JobState.PENDING_WORKER_ASSIGNMENT);
   }

  /**
   * Replaces a unassigned entry in storage with the provided {@code job} in assigned state with key
   * data.
   */
  public void updateJobStateToAssignedWithoutAuthData(String id, PublicKey publicKey,
      PrivateKey privateKey) throws IOException {
    // Verify job is in existing state
    PortabilityJob existingJob = lookupJob(id, JobState.PENDING_WORKER_ASSIGNMENT);
    Preconditions.checkArgument(existingJob != null, "Attempting to update a non-existent job");
    // Verify existing job in correct state
    Preconditions.checkState(existingJob.workerInstancePublicKey() == null);
    Preconditions.checkState(existingJob.workerInstancePrivateKey() == null);
    // Populate job with keys to persist
    String encodedPublicKey = PublicPrivateKeyPairGenerator.encodeKey(publicKey);
    String encodedPrivateKey = PublicPrivateKeyPairGenerator.encodeKey(privateKey);
    PortabilityJob updatedJob = existingJob.toBuilder()
        .setWorkerInstancePublicKey(encodedPublicKey)
        .setWorkerInstancePrivateKey(encodedPrivateKey)
        .build();
    updateJobState(updatedJob, JobState.PENDING_WORKER_ASSIGNMENT,
        JobState.ASSIGNED_WITHOUT_AUTH_DATA);
  }

  /**
   * Updates an existing assigned {@code job} with encrypted auth data.
   */
  public void updateJobStateToAssigneWithAuthData(String id, String encryptedExportAuthData,
      String encryptedImportAuthData) throws IOException {
    PortabilityJob existingJob = lookupJob(id, JobState.ASSIGNED_WITHOUT_AUTH_DATA);
    Preconditions.checkArgument(existingJob != null, "Attempting to update a non-existent job");
    Preconditions.checkState(existingJob.encryptedExportAuthData() == null);
    Preconditions.checkState(existingJob.encryptedImportAuthData() == null);
    // Populate job with encrypted auth data
    PortabilityJob updatedJob = existingJob.toBuilder()
        .setEncryptedExportAuthData(encryptedExportAuthData)
        .setEncryptedImportAuthData(encryptedImportAuthData)
        .build();
    updateJobState(updatedJob, JobState.ASSIGNED_WITHOUT_AUTH_DATA,
        JobState.ASSIGNED_WITH_AUTH_DATA);
  }

  /**
   * Deletes an existing {@code job} in storage with the given {@code jobState}.
   */
  public void deleteJob(String id, JobState jobState) throws IOException {
    String key = createKey(jobState, id);
    Map<String, Object> existing = storage.get(key);
    Preconditions.checkArgument(existing != null, "Job not found");
    storage.delete(key);
  }

  /**
   * Updates an existing {@code job} in storage with the given {@code jobState}.
   */
  private void updateJob(PortabilityJob job, JobState jobState) throws IOException {
    String key = createKey(jobState, job.id());
    Map<String, Object> existing = storage.get(key);
    Preconditions.checkArgument(existing != null, "Job not found");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    logger.debug("Data: {}", data);
    storage.put(key, data);
  }

  /**
   * Updates an existing {@code job} in storage with the given {@code jobState}.
   */
  private void updateJobState(PortabilityJob job, JobState previous, JobState updated) throws IOException {
    String previousKey = createKey(previous, job.id());
    Map<String, Object> existing = storage.get(previousKey);
    Preconditions.checkArgument(existing != null, "Job not found");

    String updatedKey = createKey(updated, job.id());
    Map<String, Object> shouldNotExist = storage.get(updatedKey);
    Preconditions.checkArgument(shouldNotExist == null, "Job in updated state already found");

    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(updatedKey, data);
    storage.delete(previousKey);
  }

  // UTILITY METHODS

  // TODO: come up with a better scheme for indexing state
  private static String createKey(JobState state, String id) {
    Preconditions.checkArgument(!id.contains(KEY_SEPERATOR));
    return String.format("%s%s%s", state.name(), KEY_SEPERATOR, id);
  }

  private static String getIdFromKey(String key) {
    Preconditions.checkArgument(key.contains(KEY_SEPERATOR));
    return key.split(KEY_SEPERATOR)[1];
  }

  private static String getString(Map<String, Object> map, String key) {
    return (String) map.get(key);
  }

  // DEPRECATED

  /**
   * Inserts a new {@code job} in storage.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public void insertJobWithKey(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing == null, "Attempting to insert an already existing job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(getString(data, ID_DATA_KEY), data);
  }

  @Deprecated
  public void insertJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing == null, "Attempting to insert an already existing job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    logger.debug("insertJob: Data: {}", data);
    storage.put(job.id(), data);
  }

  /**
   * Returns the information for a user job or null if not found.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public PortabilityJob findExistingJob(String id) {
    Preconditions.checkNotNull(id);
    Map<String, Object> data = storage.get(id);
    logger.debug("findExistingJob: id: {} data: {}", id, data);
    if (data == null || data.isEmpty()) {
      return null;
    }
    return PortabilityJob.mapToJob(data);
  }

  /**
   * Updates an existing {@code job} in storage.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public void updateJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing != null, "Attempting to update a non-existent job");
    // Store the updated job info
    logger.debug("updateJob: job: {}", job);
    Map<String, Object> data = job.asMap();
    logger.debug("updateJob: data: {}", data);
    storage.put(job.id(), data);
  }
}
