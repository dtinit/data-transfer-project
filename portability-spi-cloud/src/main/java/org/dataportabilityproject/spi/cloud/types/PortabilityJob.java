package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.Map;
import org.dataportabilityproject.types.transfer.EntityType;

/**
 * A job that will fulfill a transfer request.
 *
 * TODO: Consider having the concepts of a data "owner".
 *
 * TODO(rtannenbaum): Make all fields final and enforce non-null. Consider AutoValue or at least
 * Builder pattern.
 */
public class PortabilityJob extends EntityType {
  /**
   * The job states.
   */
  public enum State {
    NEW, COMPLETE, ERROR
  }

  // Keys for specific values in the key value store
  private static final String DATA_TYPE_KEY = "DATA_TYPE";
  private static final String EXPORT_SERVICE_KEY = "EXPORT_SERVICE";
  private static final String IMPORT_SERVICE_KEY = "IMPORT_SERVICE";
  private static final String EXPORT_ENCRYPTED_CREDS_KEY = "EXPORT_ENCRYPTED_CREDS_KEY";
  private static final String IMPORT_ENCRYPTED_CREDS_KEY = "IMPORT_ENCRYPTED_CREDS_KEY";
  private static final String ENCRYPTED_SESSION_KEY = "ENCRYPTED_SESSION_KEY";
  private static final String WORKER_INSTANCE_PUBLIC_KEY = "WORKER_INSTANCE_PUBLIC_KEY";
  private static final String WORKER_INSTANCE_PRIVATE_KEY = "WORKER_INSTANCE_PRIVATE_KEY";
  public static final String AUTHORIZATION_STATE = "AUTHORIZATION_STATE";

  @JsonProperty
  private State state = State.NEW;

  @JsonProperty
  private String exportService;

  @JsonProperty
  private String importService;

  @JsonProperty
  private String transferDataType;

  @JsonProperty
  private LocalDateTime createdTimestamp; // ISO 8601 timestamp

  @JsonProperty
  private LocalDateTime lastUpdateTimestamp; // ISO 8601 timestamp

  @JsonProperty
  private JobAuthorization jobAuthorization;

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public String getExportService() {
    return exportService;
  }

  public void setExportService(String exportService) {
    this.exportService = exportService;
  }

  public String getImportService() {
    return importService;
  }

  public void setImportService(String importService) {
    this.importService = importService;
  }

  public String getTransferDataType() {
    return transferDataType;
  }

  public void setTransferDataType(String transferDataType) {
    this.transferDataType = transferDataType;
  }

  public LocalDateTime getCreatedTimestamp() {
    return createdTimestamp;
  }

  public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  public LocalDateTime getLastUpdateTimestamp() {
    return lastUpdateTimestamp;
  }

  public void setLastUpdateTimestamp(LocalDateTime lastUpdateTimestamp) {
    this.lastUpdateTimestamp = lastUpdateTimestamp;
  }

  public JobAuthorization getJobAuthorization() {
    return jobAuthorization;
  }

  /**
   * Sets and validates the {@link JobAuthorization} data associated with this job.
   */
  public void setJobAuthorization(JobAuthorization jobAuthorization) {
    switch (jobAuthorization.getState()) {
      case INITIAL:
      case CREDS_AVAILABLE:
        // SessionKey required to create a job
        isSet(jobAuthorization.getEncryptedSessionKey());
        isUnset(jobAuthorization.getEncryptedExportAuthData(),
            jobAuthorization.getEncryptedImportAuthData(),
            jobAuthorization.getEncryptedPublicKey(),
            jobAuthorization.getEncryptedPrivateKey());
        break;
      case CREDS_ENCRYPTION_KEY_GENERATED:
        // Expected associated keys from the assigned worker to be present
        isSet(jobAuthorization.getEncryptedSessionKey(),
            jobAuthorization.getEncryptedPublicKey(),
            jobAuthorization.getEncryptedPrivateKey());
        isUnset(jobAuthorization.getEncryptedExportAuthData(),
            jobAuthorization.getEncryptedImportAuthData()
        );
        break;
      case CREDS_ENCRYPTED:
        // Expected all fields set
        isSet(jobAuthorization.getEncryptedSessionKey(),
            jobAuthorization.getEncryptedPublicKey(),
            jobAuthorization.getEncryptedPrivateKey(),
            jobAuthorization.getEncryptedExportAuthData(),
            jobAuthorization.getEncryptedImportAuthData());
        break;
    }
    this.jobAuthorization = jobAuthorization;
  }

  public Map<String, Object> toMap() {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    if (!Strings.isNullOrEmpty(transferDataType)) {
      builder.put(DATA_TYPE_KEY, transferDataType);
    }
    if (!Strings.isNullOrEmpty(exportService)){
      builder.put(EXPORT_SERVICE_KEY, exportService);
    }
    if (!Strings.isNullOrEmpty(importService)){
      builder.put(IMPORT_SERVICE_KEY, importService);
    }
    if (null != jobAuthorization.getEncryptedExportAuthData()) {
      builder.put(EXPORT_ENCRYPTED_CREDS_KEY, jobAuthorization.getEncryptedExportAuthData());
    }
    if (null != jobAuthorization.getEncryptedImportAuthData()) {
      builder.put(IMPORT_ENCRYPTED_CREDS_KEY, jobAuthorization.getEncryptedImportAuthData());
    }
    if (null != jobAuthorization.getEncryptedSessionKey()) {
      builder.put(ENCRYPTED_SESSION_KEY, jobAuthorization.getEncryptedSessionKey());
    }
    if (null != jobAuthorization.getEncryptedPublicKey()) {
      builder.put(WORKER_INSTANCE_PUBLIC_KEY, jobAuthorization.getEncryptedPublicKey());
    }
    if (null != jobAuthorization.getEncryptedPrivateKey()) {
      builder.put(WORKER_INSTANCE_PRIVATE_KEY, jobAuthorization.getEncryptedPrivateKey());
    }
    if (null != jobAuthorization.getState()) {
      builder.put(AUTHORIZATION_STATE, jobAuthorization.getState());
    }
    return builder.build();
  }

  public static PortabilityJob fromMap(Map<String, Object> properties) {
    PortabilityJob portabilityJob = new PortabilityJob();
    JobAuthorization jobAuthorization = new JobAuthorization();

    if (properties.containsKey(DATA_TYPE_KEY)) {
      portabilityJob.setTransferDataType((String) properties.get(DATA_TYPE_KEY));
    }
    if (properties.containsKey(EXPORT_SERVICE_KEY)) {
      portabilityJob.setExportService((String) properties.get(EXPORT_SERVICE_KEY));
    }
    if (properties.containsKey(IMPORT_SERVICE_KEY)) {
      portabilityJob.setImportService((String) properties.get(IMPORT_SERVICE_KEY));
    }
    if (properties.containsKey(EXPORT_ENCRYPTED_CREDS_KEY)) {
      jobAuthorization.setEncryptedExportAuthData(
          (String) properties.get(EXPORT_ENCRYPTED_CREDS_KEY));
    }
    if (properties.containsKey(IMPORT_ENCRYPTED_CREDS_KEY)) {
      jobAuthorization.setEncryptedImportAuthData(
          (String) properties.get(IMPORT_ENCRYPTED_CREDS_KEY));
    }
    if (properties.containsKey(ENCRYPTED_SESSION_KEY)) {
      jobAuthorization.setEncryptedSessionKey((String) properties.get(ENCRYPTED_SESSION_KEY));
    }
    if (properties.containsKey(WORKER_INSTANCE_PUBLIC_KEY)) {
      jobAuthorization.setEncryptedPublicKey((String) properties.get(WORKER_INSTANCE_PUBLIC_KEY));
    }
    if (properties.containsKey(WORKER_INSTANCE_PRIVATE_KEY)) {
      jobAuthorization.setEncryptedPrivateKey((String) properties.get(WORKER_INSTANCE_PRIVATE_KEY));
    }
    if (properties.containsKey(AUTHORIZATION_STATE)) {
      jobAuthorization.setState(
          JobAuthorization.State.valueOf((String) properties.get(AUTHORIZATION_STATE)));
    }
    portabilityJob.setJobAuthorization(jobAuthorization);
    return portabilityJob;
  }

  /** Checks all {@code strings} are null or empty. */
  private static void isUnset(String... strings) {
    for (String str : strings) {
      Preconditions.checkState(Strings.isNullOrEmpty(str));
    }
  }

  /** Checks all {@code strings} are have non-null and non-empty values. */
  private static void isSet(String... strings) {
    for (String str : strings) {
      Preconditions.checkState(!Strings.isNullOrEmpty(str));
    }
  }
}
