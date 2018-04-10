package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Authorization data related to a job. */
@AutoValue
@JsonDeserialize(builder = JobAuthorization.Builder.class)
public abstract class JobAuthorization {
  public static Builder builder() {
    // TODO: Fix so we don't need fully qualified name here. This is to get IntelliJ to recognize
    // the class name due to a conflict in package names for our generated code, but the conflict
    // doesn't cause any actual problems with building.
    return new org.dataportabilityproject.spi.cloud.types.AutoValue_JobAuthorization.Builder()
        .setState(State.INITIAL);
  }

  @JsonProperty("state")
  public abstract JobAuthorization.State state();

  @Nullable
  @JsonProperty("encryptedInitialExportAuthData")
  public abstract String encryptedInitialExportAuthData();

  @Nullable
  @JsonProperty("encryptedInitialImportAuthData")
  public abstract String encryptedInitialImportAuthData();

  @Nullable
  @JsonProperty("encryptedExportAuthData")
  public abstract String encryptedExportAuthData();

  @Nullable
  @JsonProperty("encryptedImportAuthData")
  public abstract String encryptedImportAuthData();

  /** The SecretKey used to encrypt all data, including auth data, associated with this job, encoded for storage. */
  @JsonProperty("sessionSecretKey")
  public abstract String sessionSecretKey();

  /**
   * The SecretKey used to symmetrically encrypt auth data. Must be encrypted with the authPublicKey
   * before storage.
   */
  @Nullable
  @JsonProperty("authSecretKey")
  public abstract String authSecretKey();

  /** The PublicKey of the 'worker' instance assigned to this job, encoded for storage. */
  @Nullable
  @JsonProperty("authPublicKey")
  public abstract String authPublicKey();

  public abstract Builder toBuilder();

  /** The current authorization state of the job. */
  public enum State {
    // The job is in the process of obtaining export and import authorization credentials via the
    // api auth flow.
    INITIAL,
    // The api authorization flow has completed and raw credentials are temporarily available in
    // the api server.
    CREDS_AVAILABLE,
    // A worker has spun up and generated a key to encrypt the credentials above so that it (alone)
    // may use them.
    CREDS_ENCRYPTION_KEY_GENERATED,
    // The api server has encrypted the credentials for the worker to use.
    CREDS_ENCRYPTED,
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonCreator
    private static Builder create() {
      return JobAuthorization.builder();
    }

    @JsonProperty("state")
    public abstract Builder setState(JobAuthorization.State state);

    @JsonProperty("encryptedInitialExportAuthData")
    public abstract Builder setEncryptedInitialExportAuthData(String authData);

    @JsonProperty("encryptedInitialImportAuthData")
    public abstract Builder setEncryptedInitialImportAuthData(String authData);

    @JsonProperty("encryptedExportAuthData")
    public abstract Builder setEncryptedExportAuthData(String authData);

    @JsonProperty("encryptedImportAuthData")
    public abstract Builder setEncryptedImportAuthData(String authData);

    /** The SecretKey used to encrypt all data, including auth data, associated with this job, encoded for storage. */
    @JsonProperty("sessionSecretKey")
    public abstract Builder setSessionSecretKey(String sessionSecretKey);

    /**
     * The SecretKey used to symmetrically encrypt auth data. Must be encrypted with the authPublicKey
     * before storage.
     */
    @JsonProperty("authSecretKey")
    public abstract Builder setAuthSecretKey(String authSecretKey);

    /** The PublicKey of the 'worker' instance assigned to this job, encoded for storage. */
    @JsonProperty("authPublicKey")
    public abstract Builder setAuthPublicKey(String authPublicKey);

    public abstract JobAuthorization build();
  }
}
