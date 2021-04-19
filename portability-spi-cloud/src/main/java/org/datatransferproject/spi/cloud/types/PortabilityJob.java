package org.datatransferproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.Nullable;

/**
 * A job that will fulfill a transfer request.
 *
 * <p>TODO: Consider having the concepts of a data "owner".
 */
@AutoValue
@JsonDeserialize(builder = PortabilityJob.Builder.class)
public abstract class PortabilityJob {
  public static final String AUTHORIZATION_STATE = "AUTHORIZATION_STATE";
  // Keys for specific values in the key value store
  private static final String DATA_TYPE_KEY = "DATA_TYPE";
  private static final String EXPORT_SERVICE_KEY = "EXPORT_SERVICE";
  private static final String IMPORT_SERVICE_KEY = "IMPORT_SERVICE";
  private static final String EXPORT_INFORMATION_KEY = "EXPORT_INFORMATION";
  private static final String ENCRYPTED_CREDS_KEY = "ENCRYPTED_CREDS_KEY";
  private static final String ENCRYPTED_SESSION_KEY = "ENCRYPTED_SESSION_KEY";
  private static final String ENCRYPTION_SCHEME = "ENCRYPTION_SCHEME";
  private static final String WORKER_INSTANCE_PUBLIC_KEY = "WORKER_INSTANCE_PUBLIC_KEY";
  private static final String WORKER_INSTANCE_ID = "INSTANCE_ID";
  private static final String IMPORT_ENCRYPTED_INITIAL_AUTH_DATA =
      "IMPORT_ENCRYPTED_INITIAL_AUTH_DATA";
  private static final String EXPORT_ENCRYPTED_INITIAL_AUTH_DATA =
      "EXPORT_ENCRYPTED_INITIAL_AUTH_DATA";
  private static final String JOB_STATE = "JOB_STATE";
  private static final String FAILURE_REASON = "FAILURE_REASON";
  private static final String NUMBER_OF_FAILED_FILES_KEY = "NUM_FAILED_FILES";
  private static final String USER_TIMEZONE = "USER_TIMEZONE";
  private static final String USER_LOCALE = "USER_LOCALE";

  public static PortabilityJob.Builder builder() {
    Instant now = Instant.now();
    // TODO: Fix so we don't need fully qualified name here. This is to get IntelliJ to recognize
    // the class name due to a conflict in package names for our generated code, but the conflict
    // doesn't cause any actual problems with building.
    return new org.datatransferproject.spi.cloud.types.AutoValue_PortabilityJob.Builder()
        .setState(State.NEW)
        .setCreatedTimestamp(now)
        .setLastUpdateTimestamp(now)
        .setFailureReason(null);
  }

  public static PortabilityJob fromMap(Map<String, Object> properties) {
    Instant now = Instant.now();
    String encryptedAuthData =
        properties.containsKey(ENCRYPTED_CREDS_KEY)
            ? (String) properties.get(ENCRYPTED_CREDS_KEY)
            : null;
    String encodedPublicKey =
        properties.containsKey(WORKER_INSTANCE_PUBLIC_KEY)
            ? (String) properties.get(WORKER_INSTANCE_PUBLIC_KEY)
            : null;

    String instanceId =
        properties.containsKey(WORKER_INSTANCE_ID)
            ? (String) properties.get(WORKER_INSTANCE_ID)
            : null;

    String encryptedExportInitialAuthData =
        properties.containsKey(EXPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            ? (String) properties.get(EXPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            : null;

    String encryptedImportInitialAuthData =
        properties.containsKey(IMPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            ? (String) properties.get(IMPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            : null;

    State state =
        properties.containsKey(JOB_STATE)
            ? State.valueOf((String) properties.get(JOB_STATE))
            : State.NEW;

    String failureReason =
        properties.containsKey(FAILURE_REASON) ? (String) properties.get(FAILURE_REASON) : null;

    TimeZone userTimeZone =
        properties.containsKey(USER_TIMEZONE) ? (TimeZone) properties.get(USER_TIMEZONE) : null;

    String userLocale =
        properties.containsKey(USER_LOCALE) ? (String) properties.get(USER_LOCALE) : null;

    return PortabilityJob.builder()
        .setState(state)
        .setExportService((String) properties.get(EXPORT_SERVICE_KEY))
        .setImportService((String) properties.get(IMPORT_SERVICE_KEY))
        .setTransferDataType((String) properties.get(DATA_TYPE_KEY))
        .setExportInformation((String) properties.get(EXPORT_INFORMATION_KEY))
        .setCreatedTimestamp(now) // TODO: get from DB
        .setLastUpdateTimestamp(now)
        .setFailureReason(failureReason)
        .setJobAuthorization(
            JobAuthorization.builder()
                .setState(
                    JobAuthorization.State.valueOf((String) properties.get(AUTHORIZATION_STATE)))
                .setEncryptionScheme((String) properties.get(ENCRYPTION_SCHEME))
                .setEncryptedAuthData(encryptedAuthData)
                .setInstanceId(instanceId)
                .setSessionSecretKey((String) properties.get(ENCRYPTED_SESSION_KEY))
                .setAuthPublicKey(encodedPublicKey)
                .setEncryptedInitialExportAuthData(encryptedExportInitialAuthData)
                .setEncryptedInitialImportAuthData(encryptedImportInitialAuthData)
                .build())
        .setUserTimeZone(userTimeZone)
        .setUserLocale(userLocale)
        .build();
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

  @JsonProperty("state")
  public abstract State state();

  @JsonProperty("exportService")
  public abstract String exportService();

  @JsonProperty("importService")
  public abstract String importService();

  @JsonProperty("transferDataType")
  public abstract String transferDataType();

  @Nullable
  @JsonProperty("exportInformation")
  public abstract String exportInformation();

  @JsonProperty("createdTimestamp")
  public abstract Instant createdTimestamp(); // ISO 8601 timestamp

  @JsonProperty("lastUpdateTimestamp")
  public abstract Instant lastUpdateTimestamp(); // ISO 8601 timestamp

  @JsonProperty("jobAuthorization")
  public abstract JobAuthorization jobAuthorization();

  @Nullable
  @JsonProperty("failureReason")
  public abstract String failureReason();

  @Nullable
  @JsonProperty("userTimeZone")
  public abstract TimeZone userTimeZone();

  @Nullable
  @JsonProperty("userLocale")
  public abstract String userLocale();

  public abstract PortabilityJob.Builder toBuilder();

  public Map<String, Object> toMap() {
    ImmutableMap.Builder<String, Object> builder =
        ImmutableMap.<String, Object>builder()
            .put(DATA_TYPE_KEY, transferDataType())
            .put(EXPORT_SERVICE_KEY, exportService())
            .put(IMPORT_SERVICE_KEY, importService())
            .put(AUTHORIZATION_STATE, jobAuthorization().state().toString())
            .put(JOB_STATE, state().toString());
    if (jobAuthorization().sessionSecretKey() != null) {
      builder.put(ENCRYPTED_SESSION_KEY, jobAuthorization().sessionSecretKey());
    }

    if (null != exportInformation()) {
      builder.put(EXPORT_INFORMATION_KEY, exportInformation());
    }

    if (null != jobAuthorization().authPublicKey()) {
      builder.put(WORKER_INSTANCE_PUBLIC_KEY, jobAuthorization().authPublicKey());
    }

    if (null != jobAuthorization().instanceId()) {
      builder.put(WORKER_INSTANCE_ID, jobAuthorization().instanceId());
    }

    if (null != jobAuthorization().encryptedAuthData()) {
      builder.put(ENCRYPTED_CREDS_KEY, jobAuthorization().encryptedAuthData());
    }

    if (null != failureReason()) {
      builder.put(FAILURE_REASON, failureReason());
    }

    builder.put(
        ENCRYPTION_SCHEME,
        null != jobAuthorization().encryptionScheme()
            ? jobAuthorization().encryptionScheme()
            : "jwe");

    if (null != jobAuthorization().encryptedInitialExportAuthData()) {
      builder.put(
          EXPORT_ENCRYPTED_INITIAL_AUTH_DATA, jobAuthorization().encryptedInitialExportAuthData());
    }

    if (null != jobAuthorization().encryptedInitialImportAuthData()) {
      builder.put(
          IMPORT_ENCRYPTED_INITIAL_AUTH_DATA, jobAuthorization().encryptedInitialImportAuthData());
    }

    if (null != userTimeZone()) {
      builder.put(USER_TIMEZONE, userTimeZone());
    }

    if (null != userLocale()) {
      builder.put(USER_LOCALE, userLocale());
    }

    return builder.build();
  }

  /** The job states. */
  public enum State {
    NEW,
    IN_PROGRESS,
    COMPLETE,
    ERROR,
    CANCELED,
    PREEMPTED
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonCreator
    private static PortabilityJob.Builder create() {
      return PortabilityJob.builder();
    }

    public abstract PortabilityJob build();

    @JsonProperty("state")
    public abstract Builder setState(State state);

    @JsonProperty("exportService")
    public abstract Builder setExportService(String exportService);

    @JsonProperty("importService")
    public abstract Builder setImportService(String importService);

    @JsonProperty("transferDataType")
    public abstract Builder setTransferDataType(String transferDataType);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("exportInformation")
    public abstract Builder setExportInformation(String exportInformation);

    @JsonProperty("createdTimestamp")
    public abstract Builder setCreatedTimestamp(Instant createdTimestamp);

    @JsonProperty("lastUpdateTimestamp")
    public abstract Builder setLastUpdateTimestamp(Instant lastUpdateTimestamp);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("failureReason")
    public abstract Builder setFailureReason(String failureReason);

    @JsonProperty("jobAuthorization")
    public Builder setAndValidateJobAuthorization(JobAuthorization jobAuthorization) {
      switch (jobAuthorization.state()) {
        case INITIAL:
        case CREDS_AVAILABLE:
          isUnset(jobAuthorization.encryptedAuthData());
          break;
        case CREDS_ENCRYPTION_KEY_GENERATED:
          // Expected associated keys from the assigned transfer worker to be present
          isSet(jobAuthorization.authPublicKey());
          isUnset(jobAuthorization.encryptedAuthData());
          break;
        case CREDS_STORED:
          isSet(jobAuthorization.authPublicKey(), jobAuthorization.encryptedAuthData());
          break;
        case TIMED_OUT:
          throw new RuntimeException("Authorization timed out, can't validate.");
      }
      return setJobAuthorization(jobAuthorization);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("userTimeZone")
    public abstract Builder setUserTimeZone(TimeZone timeZone);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("userLocale")
    public abstract Builder setUserLocale(String locale);

    // For internal use only; clients should use setAndValidateJobAuthorization
    protected abstract Builder setJobAuthorization(JobAuthorization jobAuthorization);
  }
}
