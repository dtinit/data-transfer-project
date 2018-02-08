package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.dataportabilityproject.types.transfer.EntityType;

import java.time.LocalDateTime;

/**
 * A job that will fulfill a transfer request.
 *
 * TODO: Consider having the concepts of an data "owner".
 */
public class PortabilityJob extends EntityType {

  /**
   * The job states.
   */
  public enum State {
    NEW, COMPLETE, ERROR
  }

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
      case PENDING_WORKER_ASSIGNMENT:
        // SessionKey required to create a job
        isSet(jobAuthorization.getEncryptedSessionKey());
        isUnset(jobAuthorization.getEncryptedExportAuthData(),
            jobAuthorization.getEncryptedImportAuthData(),
            jobAuthorization.getEncryptedPublicKey(),
            jobAuthorization.getEncryptedPrivateKey());
        break;
      case ASSIGNED_WITHOUT_AUTH_DATA:
        // Expected associated keys from the assigned worker to be present
        isSet(jobAuthorization.getEncryptedSessionKey(),
            jobAuthorization.getEncryptedPublicKey(),
            jobAuthorization.getEncryptedPrivateKey());
        isUnset(jobAuthorization.getEncryptedExportAuthData(),
            jobAuthorization.getEncryptedImportAuthData()
        );
        break;
      case ASSIGNED_WITH_AUTH_DATA:
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
