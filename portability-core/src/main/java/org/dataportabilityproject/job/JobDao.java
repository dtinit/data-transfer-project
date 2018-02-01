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
    return storage.getFirst(JobState.PENDING_WORKER_ASSIGNMENT);
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

  /**
   * Look up a job based on ID and verify it is in the expected job state.
   */
  private PortabilityJob lookupJob(String id, JobState jobState) {
    Preconditions.checkNotNull(id);
    PortabilityJob job = storage.get(id);
    Preconditions.checkArgument(job == null || job.jobState() == jobState);
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
    job = job.toBuilder().setJobState(JobState.PENDING_AUTH_DATA).build();
    storage.put(job.id(), job);
  }

  // UPDATE METHODS

  /**
   * Updates an existing incomplete {@code job} in storage.
   */
  public void updatePendingAuthDataJob(PortabilityJob job) throws IOException {
    storage.atomicUpdate(job.id(), JobState.PENDING_AUTH_DATA, job);
  }

  /**
   * Replaces the job in PENDING_AUTH_DATA state to PENDING_WORKER_ASSIGNMENT state with
   * encryption data included.
   */
  public void updateJobStateToPendingWorkerAssignment(String id) throws IOException {
    // Lookup job so we can append to its existing properties. updateJobState will verify that
    // the job still has the expected state when performing the atomic update.
    PortabilityJob existingJob = storage.get(id);
    // No updated to the job itself, just the state
    PortabilityJob updatedJob =
        existingJob.toBuilder().setJobState(JobState.PENDING_WORKER_ASSIGNMENT).build();
    updateJobState(updatedJob, JobState.PENDING_AUTH_DATA);
   }

  /**
   * Replaces a unassigned entry in storage with the provided {@code job} in assigned state with key
   * data. Returns whether it was able to update the job.
   */
  public void updateJobStateToAssignedWithoutAuthData(String id, PublicKey publicKey,
      PrivateKey privateKey) throws IOException {
    // Lookup job so we can append to its existing properties. updateJobState will verify that
    // the job still has the expected state when performing the atomic update.
    PortabilityJob existingJob = storage.get(id);
    // Verify no worker key
    Preconditions.checkState(existingJob.workerInstancePublicKey() == null);
    Preconditions.checkState(existingJob.workerInstancePrivateKey() == null);
    // Populate job with keys to persist
    String encodedPublicKey = PublicPrivateKeyPairGenerator.encodeKey(publicKey);
    String encodedPrivateKey = PublicPrivateKeyPairGenerator.encodeKey(privateKey);
    PortabilityJob updatedJob = existingJob.toBuilder()
        .setWorkerInstancePublicKey(encodedPublicKey)
        .setWorkerInstancePrivateKey(encodedPrivateKey)
        .setJobState(JobState.ASSIGNED_WITHOUT_AUTH_DATA)
        .build();
    updateJobState(updatedJob, JobState.PENDING_WORKER_ASSIGNMENT);
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
        .setJobState(JobState.ASSIGNED_WITH_AUTH_DATA)
        .build();
    updateJobState(updatedJob, JobState.ASSIGNED_WITHOUT_AUTH_DATA);
  }

  /**
   * Deletes an existing {@code job} in storage with the given {@code jobState}.
   */
  public void deleteJob(String jobId) throws IOException {
    storage.delete(jobId);
  }

  /**
   * Atomically updates an existing {@code job} in storage with the given {@code jobState}.
   *
   * @throws IOException if the update failed.
   */
  private void updateJobState(PortabilityJob job, JobState previousJobState) throws IOException {
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.atomicUpdate(job.id(), previousJobState, job);
  }

  // UTILITY METHODS

  // DEPRECATED
  /**
   * Updates an existing {@code job} in storage.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public void updateJob(PortabilityJob job) throws IOException {
    storage.atomicUpdate(job.id(), null, job);
  }

  @Deprecated
  public void insertJob(PortabilityJob job) throws IOException {
    storage.put(job.id(), job);
  }

  /**
   * Returns the information for a user job or null if not found.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public PortabilityJob findExistingJob(String id) {
    Preconditions.checkNotNull(id);
    PortabilityJob job = storage.get(id);
    logger.debug("findExistingJob: id: {} job: {}", id, job);
    return job;
  }
}
