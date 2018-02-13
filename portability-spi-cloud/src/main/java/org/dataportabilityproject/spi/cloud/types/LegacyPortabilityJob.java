package org.dataportabilityproject.spi.cloud.types;

import com.google.auto.value.AutoValue;
import java.util.Map;
import javax.annotation.Nullable;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/** Data about a particular portability job. */
/**
 * TODO(rtannenbaum): Migrate usage of this to the new {@link PortabilityJob}
 */
@AutoValue
public abstract class LegacyPortabilityJob {

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
  @Nullable public abstract JobAuthorization.State jobState();

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
    public abstract LegacyPortabilityJob.Builder setJobState(JobAuthorization.State jobState);

    abstract LegacyPortabilityJob autoBuild(); // not public

    /** Validates required values on build. */
    public LegacyPortabilityJob build() {
      return autoBuild();
    }
  }

  /** Represents this job as Map of key value pairs. */
  public Map<String, Object> asMap() {
    return new LegacyPortabilityJobConverter().doForward(this);
  }

  /** Creates a {@link LegacyPortabilityJob} from the data in the given {@code map}. */
  public static LegacyPortabilityJob mapToJob(Map<String, Object> map) {
    return new LegacyPortabilityJobConverter().doBackward(map);
  }
}

