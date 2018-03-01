package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * Authorization data related to a job.
 */
@AutoValue
@JsonDeserialize(builder = JobAuthorization.Builder.class)
public abstract class JobAuthorization {
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

  @JsonProperty("encryptedSessionKey")
  public abstract String encryptedSessionKey();

  @Nullable
  @JsonProperty("encryptedPublicKey")
  public abstract String encryptedPublicKey();

  @Nullable
  @JsonProperty("encryptedPrivateKey")
  public abstract String encryptedPrivateKey();

  public static Builder builder() {
    // TODO: Fix so we don't need fully qualified name here. This is to get IntelliJ to recognize
    // the class name due to a conflict in package names for our generated code, but the conflict
    // doesn't cause any actual problems with building.
    return new org.dataportabilityproject.spi.cloud.types.AutoValue_JobAuthorization.Builder()
        .setState(State.INITIAL);
  }

  public abstract Builder toBuilder();

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

    @JsonProperty("encryptedSessionKey")
    public abstract Builder setEncryptedSessionKey(String sessionKey);

    @JsonProperty("encryptedPublicKey")
    public abstract Builder setEncryptedPublicKey(String publicKey);

    @JsonProperty("encryptedPrivateKey")
    public abstract Builder setEncryptedPrivateKey(String privateKey);

    public abstract JobAuthorization build();
  }
}
