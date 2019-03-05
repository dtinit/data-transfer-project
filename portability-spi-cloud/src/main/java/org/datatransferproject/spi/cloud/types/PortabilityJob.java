package org.datatransferproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.types.common.ExportInformation;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

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
  private static final String IMPORT_ENCRYPTED_INITIAL_AUTH_DATA =
      "IMPORT_ENCRYPTED_INITIAL_AUTH_DATA";
  private static final String EXPORT_ENCRYPTED_INITIAL_AUTH_DATA =
      "EXPORT_ENCRYPTED_INITIAL_AUTH_DATA";

  public static PortabilityJob.Builder builder() {
    LocalDateTime now = LocalDateTime.now();
    // TODO: Fix so we don't need fully qualified name here. This is to get IntelliJ to recognize
    // the class name due to a conflict in package names for our generated code, but the conflict
    // doesn't cause any actual problems with building.
    return new org.datatransferproject.spi.cloud.types.AutoValue_PortabilityJob.Builder()
        .setState(State.NEW)
        .setCreatedTimestamp(now)
        .setLastUpdateTimestamp(now);
  }

  public static PortabilityJob fromMap(Map<String, Object> properties) {
    LocalDateTime now = LocalDateTime.now();
    String encryptedAuthData =
        properties.containsKey(ENCRYPTED_CREDS_KEY)
            ? (String) properties.get(ENCRYPTED_CREDS_KEY)
            : null;
    String encodedPublicKey =
        properties.containsKey(WORKER_INSTANCE_PUBLIC_KEY)
            ? (String) properties.get(WORKER_INSTANCE_PUBLIC_KEY)
            : null;

    String encryptedExportInitialAuthData =
        properties.containsKey(EXPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            ? (String) properties.get(EXPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            : null;

    String encryptedImportInitialAuthData =
        properties.containsKey(IMPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            ? (String) properties.get(IMPORT_ENCRYPTED_INITIAL_AUTH_DATA)
            : null;

    return PortabilityJob.builder()
        .setState(State.NEW)
        .setExportService((String) properties.get(EXPORT_SERVICE_KEY))
        .setImportService((String) properties.get(IMPORT_SERVICE_KEY))
        .setTransferDataType((String) properties.get(DATA_TYPE_KEY))
        .setExportInformation((ExportInformation) properties.get(EXPORT_INFORMATION_KEY))
        .setCreatedTimestamp(now) // TODO: get from DB
        .setLastUpdateTimestamp(now)
        .setJobAuthorization(
            JobAuthorization.builder()
                .setState(
                    JobAuthorization.State.valueOf((String) properties.get(AUTHORIZATION_STATE)))
                .setEncryptionScheme((String) properties.get(ENCRYPTION_SCHEME))
                .setEncryptedAuthData(encryptedAuthData)
                .setSessionSecretKey((String) properties.get(ENCRYPTED_SESSION_KEY))
                .setAuthPublicKey(encodedPublicKey)
                .setEncryptedInitialExportAuthData(encryptedExportInitialAuthData)
                .setEncryptedInitialImportAuthData(encryptedImportInitialAuthData)
                .build())
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
  @JsonProperty(value = "exportInformation")
  public abstract ExportInformation exportInformation();

  @JsonProperty("createdTimestamp")
  public abstract LocalDateTime createdTimestamp(); // ISO 8601 timestamp

  @JsonProperty("lastUpdateTimestamp")
  public abstract LocalDateTime lastUpdateTimestamp(); // ISO 8601 timestamp

  @JsonProperty("jobAuthorization")
  public abstract JobAuthorization jobAuthorization();

  public abstract PortabilityJob.Builder toBuilder();

  public Map<String, Object> toMap() {
    ImmutableMap.Builder<String, Object> builder =
        ImmutableMap.<String, Object>builder()
            .put(DATA_TYPE_KEY, transferDataType())
            .put(EXPORT_SERVICE_KEY, exportService())
            .put(IMPORT_SERVICE_KEY, importService())
            .put(AUTHORIZATION_STATE, jobAuthorization().state().toString());
    if (jobAuthorization().sessionSecretKey() != null) {
      builder.put(ENCRYPTED_SESSION_KEY, jobAuthorization().sessionSecretKey());
    }

    if (null != exportInformation()) {
      builder.put(EXPORT_INFORMATION_KEY, exportInformation());
    }

    if (null != jobAuthorization().authPublicKey()) {
      builder.put(WORKER_INSTANCE_PUBLIC_KEY, jobAuthorization().authPublicKey());
    }

    if (null != jobAuthorization().encryptedAuthData()) {
      builder.put(ENCRYPTED_CREDS_KEY, jobAuthorization().encryptedAuthData());
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
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PortabilityJob that = (PortabilityJob) o;
    return Objects.equals(state(), that.state()) &&
            Objects.equals(exportService(), that.exportService()) &&
            Objects.equals(importService(), that.importService()) &&
            Objects.equals(transferDataType(), that.transferDataType()) &&
            Objects.equals(exportInformation(), that.exportInformation()) &&
            Objects.equals(createdTimestamp(), that.createdTimestamp()) &&
            Objects.equals(lastUpdateTimestamp(), that.lastUpdateTimestamp()) &&
            Objects.equals(jobAuthorization(), that.jobAuthorization());
  }

  /** The job states. */
  public enum State {
    NEW,
    COMPLETE,
    ERROR
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
    public abstract Builder setExportInformation(ExportInformation exportInformation);

    @JsonProperty("createdTimestamp")
    public abstract Builder setCreatedTimestamp(LocalDateTime createdTimestamp);

    @JsonProperty("lastUpdateTimestamp")
    public abstract Builder setLastUpdateTimestamp(LocalDateTime lastUpdateTimestamp);

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
          isSet(
              jobAuthorization.authPublicKey(),
              jobAuthorization.encryptedAuthData());
          break;
      }
      return setJobAuthorization(jobAuthorization);
    }

    // For internal use only; clients should use setAndValidateJobAuthorization
    protected abstract Builder setJobAuthorization(JobAuthorization jobAuthorization);
  }
}
