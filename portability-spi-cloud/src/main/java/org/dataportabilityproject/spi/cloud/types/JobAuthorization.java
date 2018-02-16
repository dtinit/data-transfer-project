package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dataportabilityproject.types.transfer.EntityType;

/**
 * Authorization data related to a job.
 */
public class JobAuthorization extends EntityType {

  /**
   * The current authorization state of the job.
   */
  public enum State {
    // The job is in the process of obtaining export and import authorization credentials via the
    // gateway auth flow.
    INITIAL,
    // The gateway authorization flow has completed and raw credentials are temporarily available in
    // the gateway server.
    CREDS_AVAILABLE,
    // A worker has spun up and generated a key to encrypt the credentials above so that it (alone)
    // may use them.
    CREDS_ENCRYPTION_KEY_GENERATED,
    // The gateway server has encrypted the credentials for the worker to use.
    CREDS_ENCRYPTED,
  }

  @JsonProperty
  private State state;

  @JsonProperty
  private String encryptedExportAuthData;

  @JsonProperty
  private String encryptedImportAuthData;

  @JsonProperty
  private String encryptedSessionKey;

  @JsonProperty
  private String encryptedPublicKey;

  @JsonProperty
  private String encryptedPrivateKey;

  public String getEncryptedExportAuthData() {
    return encryptedExportAuthData;
  }

  public void setEncryptedExportAuthData(String encryptedExportAuthData) {
    this.encryptedExportAuthData = encryptedExportAuthData;
  }

  public String getEncryptedImportAuthData() {
    return encryptedImportAuthData;
  }

  public void setEncryptedImportAuthData(String encryptedImportAuthData) {
    this.encryptedImportAuthData = encryptedImportAuthData;
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
