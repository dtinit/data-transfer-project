package org.dataportabilityproject.job;

import com.google.auto.value.AutoValue;
import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Map;
import javax.annotation.Nullable;
import org.dataportabilityproject.shared.auth.AuthData;

/** Data about a particular portability job. */
@AutoValue
public abstract class PortabilityJob {
  public abstract String id();
  @Nullable public abstract String dataType();
  @Nullable public abstract String exportService();
  @Nullable public abstract String exportAccount();
  @Nullable public abstract AuthData exportInitialAuthData();
  @Nullable public abstract AuthData exportAuthData();
  @Nullable public abstract String importService();
  @Nullable public abstract String importAccount();
  @Nullable public abstract AuthData importInitialAuthData();
  @Nullable public abstract AuthData importAuthData();

  public static AutoValue_PortabilityJob.Builder builder() {
     return new AutoValue_PortabilityJob.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);
    public abstract Builder setDataType(String id);
    public abstract Builder setExportService(String id);
    public abstract Builder setExportAccount(String id);
    public abstract Builder setExportInitialAuthData(AuthData id);
    public abstract Builder setExportAuthData(AuthData id);
    public abstract Builder setImportService(String id);
    public abstract Builder setImportAccount(String id);
    public abstract Builder setImportInitialAuthData(AuthData id);
    public abstract Builder setImportAuthData(AuthData id);

    abstract PortabilityJob autoBuild(); // not public

    /** Validates required values on build. */
    public PortabilityJob build() {
      PortabilityJob job = autoBuild();
      Preconditions.checkState(!Strings.isNullOrEmpty(job.id()), "Invalid id value");
      return job;
    }
  }

  /** Represents this job as Map of key value pairs. */
  public Map<String, Object> asMap() {
    return new PortabilityJobConverter().doForward(this);
  }

  /** Creates a {@link PortabilityJob} from the data in the given {@code map}. */
  public static PortabilityJob mapToJob(Map<String, Object> map) {
    return new PortabilityJobConverter().doBackward(map);
  }
}
