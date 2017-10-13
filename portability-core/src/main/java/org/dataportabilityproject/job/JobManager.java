package org.dataportabilityproject.job;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

/** Provides functionality to manage the lifecycle of a data portability job. */
public class JobManager {
  // Keys for specific values in data store
  private static final String ID_DATA_KEY = "UUID";
  private static final String DATA_TYPE_DATA_KEY = "DATA_TYPE";
  private static final String EXPORT_SERVICE_DATA_KEY = "EXPORT_SERVICE";
  private static final String EXPORT_ACCOUNT_DATA_KEY = "EXPORT_ACCOUNT";
  private static final String EXPORT_INITIAL_AUTH_DATA_KEY = "EXPORT_INITIAL_AUTH_DATA";
  private static final String EXPORT_AUTH_DATA_KEY = "EXPORT_AUTH_DATA";
  private static final String IMPORT_SERVICE_DATA_KEY = "IMPORT_SERVICE";
  private static final String IMPORT_ACCOUNT_DATA_KEY = "IMPORT_ACCOUNT";
  private static final String IMPORT_INITIAL_AUTH_DATA_KEY = "IMPORT_INITIAL_AUTH_DATA";
  private static final String IMPORT_AUTH_DATA_KEY = "IMPORT_AUTH_DATA";

  private final PersistentKeyValueStore storage;

  public JobManager(PersistentKeyValueStore storage) {
    this.storage = storage;
  }

  /** Returns the information for a user job or null if not found. */
  public PortabilityJob findExistingJob(String id) {
    Preconditions.checkNotNull(id);
    Map<String, Object> data = storage.get(id);
    if (data == null || data.isEmpty()) {
      return null;
    }
    return PortabilityJob.mapToJob(data);
  }

  /** Returns a job in unassigned state. */
  public String findUnassignedJob() {
    // TODO: Implement selecting a job in unassigned state for jobs
    // TODO: Update job to assigned state so no other worker grabs it
    throw new UnsupportedOperationException("Implement me!");
  }

  /** Replaces the existing entry in storage with the provided {@code job}. */
  public void insertJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing != null, "Attempting to updatea  non-exisent job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(getString(data, ID_DATA_KEY), data);
  }

  /** Replaces the existing entry in storage with the provided {@code job}. */
  public void updateJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing != null, "Attempting to updatea  non-exisent job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(getString(data, ID_DATA_KEY), data);
  }

  private static String getString(Map<String, Object> map, String key) {
    return (String) map.get(key);
  }

}
