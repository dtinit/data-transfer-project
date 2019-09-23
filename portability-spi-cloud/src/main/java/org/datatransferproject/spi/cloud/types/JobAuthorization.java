package org.datatransferproject.spi.cloud.types;

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
    return new org.datatransferproject.spi.cloud.types.AutoValue_JobAuthorization.Builder()
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
  @JsonProperty("encryptedAuthData")
  public abstract String encryptedAuthData();

  @Nullable
  @JsonProperty("encryptionScheme")
  public abstract String encryptionScheme();

  /**
   * The SecretKey used to encrypt all data, including auth data, associated with this job, encoded
   * for storage.
   */
  @Deprecated
  @Nullable
  @JsonProperty("sessionSecretKey")
  public abstract String sessionSecretKey();

  /** The PublicKey of the 'transfer worker' instance assigned to this job, encoded for storage. */
  @Nullable
  @JsonProperty("authPublicKey")
  public abstract String authPublicKey();

  /** The id of the 'transfer worker' instance assigned to this job */
  @Nullable
  @JsonProperty("instanceId")
  public abstract String instanceId();

  public abstract Builder toBuilder();

  /** The current authorization state of the job. */
  public enum State {
    // The job is in the process of obtaining export and import authorization credentials via the
    // api auth flow.
    INITIAL,
    // The authorization flow has completed and raw credentials are temporarily available in the
    // client.
    CREDS_AVAILABLE,
    // A transfer worker has spun up and generated a key to encrypt the credentials above so that it
    // (alone)
    // may use them.
    CREDS_ENCRYPTION_KEY_GENERATED,
    // The api server has encrypted the credentials for the transfer worker to use.
    CREDS_STORED,
    // The worker has timed out
    TIMED_OUT,
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

    /**
     * Sets the auth data using {@link org.datatransferproject.types.transfer.auth.AuthDataPair}
     * encrypted as a JWE.
     */
    @JsonProperty("encryptedAuthData")
    public abstract Builder setEncryptedAuthData(String authData);

    @JsonProperty("encryptionScheme")
    public abstract Builder setEncryptionScheme(String scheme);

    /**
     * The SecretKey used to encrypt all data, including auth data, associated with this job,
     * encoded for storage.
     */
    @Deprecated
    @JsonProperty("sessionSecretKey")
    public abstract Builder setSessionSecretKey(String sessionSecretKey);

    /**
     * The PublicKey of the 'transfer worker' instance assigned to this job, encoded for storage.
     */
    @JsonProperty("authPublicKey")
    public abstract Builder setAuthPublicKey(String authPublicKey);

    /**
     * The id of the 'transfer worker' instance assigned to this job.
     */
    @JsonProperty("instanceId")
    public abstract Builder setInstanceId(String instanceId);

    public abstract JobAuthorization build();
  }
}
