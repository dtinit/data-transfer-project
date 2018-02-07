package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.dataportabilityproject.types.transfer.EntityType;

/**
 * Authorization data related to a job.
 */
public class JobAuthorization extends EntityType {

  /**
   * The current state of the job.
   *
   * <p>The value PENDING_WORKER_ASSIGNMENT indicates the client has sent a request for a worker to
   * be assigned before sending all the data required for the job.
   *
   * <p>The value ASSIGNED_WITHOUT_AUTH_DATA indicates the client has submitted all data required,
   * such as the encrypted auth data, in order to begin processing the job.
   */
  public enum State {
    // The job is in the process of obtaining source and destination authorization credentials.
    INITIAL,
    // The job has all authorization credentials but is not assigned a worker yet
    PENDING_WORKER_ASSIGNMENT,
    // The job is assigned a worker instance, authorization credentials have not been populated.
    ASSIGNED_WITHOUT_AUTH_DATA,
    // The job is assigned a worker and has encrypted authorization credentials populated.
    ASSIGNED_WITH_AUTH_DATA,
  }

  @JsonProperty
  private JobAuthorization.State state;

  @JsonProperty
  private String encryptedSourceAuthData;

  @JsonProperty
  private String encryptedDestinationAuthData;

  @JsonProperty
  private String encryptedSessionKey;

  @JsonProperty
  private String encryptedPublicKey;

  @JsonProperty
  private String encryptedPrivateKey;

  public String getEncryptedSourceAuthData() {
    return encryptedSourceAuthData;
  }

  public void setEncryptedSourceAuthData(String encryptedSourceAuthData) {
    this.encryptedSourceAuthData = encryptedSourceAuthData;
  }

  public String getEncryptedDestinationAuthData() {
    return encryptedDestinationAuthData;
  }

  public void setEncryptedDestinationAuthData(String encryptedDestinationAuthData) {
    this.encryptedDestinationAuthData = encryptedDestinationAuthData;
  }

  public String getEncryptedSessionKey() {
    return encryptedSessionKey;
  }

  public void setEncryptedSessionKey(String encryptedSessionKey) {
    this.encryptedSessionKey = encryptedSessionKey;
  }

  public String getEncryptedPublicKey() {
    return encryptedPublicKey;
  }

  public void setEncryptedPublicKey(String encryptedPublicKey) {
    this.encryptedPublicKey = encryptedPublicKey;
  }

  public String getEncryptedPrivateKey() {
    return encryptedPrivateKey;
  }

  public void setEncryptedPrivateKey(String encryptedPrivateKey) {
    this.encryptedPrivateKey = encryptedPrivateKey;
  }

  public JobAuthorization.State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

}
