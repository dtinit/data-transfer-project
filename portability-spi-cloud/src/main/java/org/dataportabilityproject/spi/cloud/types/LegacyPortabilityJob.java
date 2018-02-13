package org.dataportabilityproject.spi.cloud.types;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Map;
import javax.annotation.Nullable;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/** Data about a particular portability job. */
/**
 * TODO(rtannenbaum): Migrate usage of this to the new {@link PortabilityJob}
 */
@AutoValue
public abstract class LegacyPortabilityJob {
  /**
   * The current state of the job.
   *
   * <p>The value PENDING_WORKER_ASSIGNMENT indicates the client has sent a request for a worker to
   * be assigned before sending all the data required for the job.
   *
   * <p>The value ASSIGNED_WITHOUT_AUTH_DATA indicates the client has submitted all data required,
   * such as the encrypted auth data, in order to begin processing the job.
   */
  public enum JobState {
    // The job has not finished the authorization flows
    PENDING_AUTH_DATA,
    // The job has all authorization information but is not assigned a worker yet
    PENDING_WORKER_ASSIGNMENT,
    // The job is assigned a worker and waiting for auth data from the api
    ASSIGNED_WITHOUT_AUTH_DATA,
    // The job is assigned a worker and has encrypted auth data
    ASSIGNED_WITH_AUTH_DATA,
  }

  @Nullable public abstract String dataType();
  @Nullable public abstract String exportService();
  @Nullable public abstract String exportAccount();
  @Nullable public abstract AuthData exportInitialAuthData();
  /** @deprecated Use encryptedExportAuthData when encrypted flow is implemented. */
  @Deprecated @Nullable public abstract AuthData exportAuthData();
  @Nullable public abstract String encryptedExportAuthData();
  @Nullable public abstract String importService();
  @Nullable public abstract String importAccount();
  @Nullable public abstract AuthData importInitialAuthData();
  /** @deprecated Use encryptedImportAuthData when encrypted flow is implemented. */
  @Deprecated @Nullable public abstract AuthData importAuthData();
  @Nullable public abstract String encryptedImportAuthData();
  @Nullable public abstract String sessionKey();
  @Nullable public abstract String workerInstancePublicKey();
  @Nullable public abstract String workerInstancePrivateKey(); // TODO: Consider removing
  // TODO: Remove Nullable - jobState should never be null after we enable encryptedFlow everywhere
  @Nullable public abstract LegacyPortabilityJob.JobState jobState();

  public static LegacyPortabilityJob.Builder builder() {
    return new AutoValue_LegacyPortabilityJob.Builder();
  }

  public abstract LegacyPortabilityJob.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract LegacyPortabilityJob.Builder setDataType(String id);
    public abstract LegacyPortabilityJob.Builder setExportService(String id);
    public abstract LegacyPortabilityJob.Builder setExportAccount(String id);
    public abstract LegacyPortabilityJob.Builder setExportInitialAuthData(AuthData id);
    /** @deprecated Use setEncryptedExportAuthData when encrypted flow is implemented. */
    @Deprecated public abstract LegacyPortabilityJob.Builder setExportAuthData(AuthData id);
    public abstract LegacyPortabilityJob.Builder setEncryptedExportAuthData(String id);
    public abstract LegacyPortabilityJob.Builder setImportService(String id);
    public abstract LegacyPortabilityJob.Builder setImportAccount(String id);
    public abstract LegacyPortabilityJob.Builder setImportInitialAuthData(AuthData id);
    /** @deprecated Use setEncryptedImportAuthData when encrypted flow is implemented. */
    @Deprecated public abstract LegacyPortabilityJob.Builder setImportAuthData(AuthData id);
    public abstract LegacyPortabilityJob.Builder setEncryptedImportAuthData(String id);
    public abstract LegacyPortabilityJob.Builder setSessionKey(String id);
    public abstract LegacyPortabilityJob.Builder setWorkerInstancePublicKey(String id);
    public abstract LegacyPortabilityJob.Builder setWorkerInstancePrivateKey(String id);
    public abstract LegacyPortabilityJob.Builder setJobState(
        LegacyPortabilityJob.JobState jobState);

    abstract LegacyPortabilityJob autoBuild(); // not public

    /** Validates required values on build. */
    public LegacyPortabilityJob build() {
      return autoBuild();
    }
  }

  /** Represents this job as Map of key value pairs. */
  public Map<String, Object> asMap() {
    return new OldPortabilityJobConverter().doForward(this);
  }

  /** Creates a {@link LegacyPortabilityJob} from the data in the given {@code map}. */
  public static LegacyPortabilityJob mapToJob(Map<String, Object> map) {
    return new OldPortabilityJobConverter().doBackward(map);
  }
}

