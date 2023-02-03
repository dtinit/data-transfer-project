package org.datatransferproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.TimeZone;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;
import org.datatransferproject.types.common.models.DataVertical;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_PortabilityJob extends PortabilityJob {

  private final PortabilityJob.State state;

  private final String exportService;

  private final String importService;

  private final DataVertical transferDataType;

  private final String exportInformation;

  private final Instant createdTimestamp;

  private final Instant lastUpdateTimestamp;

  private final JobAuthorization jobAuthorization;

  private final String failureReason;

  private final TimeZone userTimeZone;

  private final String userLocale;

  private final PortabilityJob.TransferMode transferMode;

  private AutoValue_PortabilityJob(
      PortabilityJob.State state,
      String exportService,
      String importService,
      DataVertical transferDataType,
      @Nullable String exportInformation,
      Instant createdTimestamp,
      Instant lastUpdateTimestamp,
      JobAuthorization jobAuthorization,
      @Nullable String failureReason,
      @Nullable TimeZone userTimeZone,
      @Nullable String userLocale,
      @Nullable PortabilityJob.TransferMode transferMode) {
    this.state = state;
    this.exportService = exportService;
    this.importService = importService;
    this.transferDataType = transferDataType;
    this.exportInformation = exportInformation;
    this.createdTimestamp = createdTimestamp;
    this.lastUpdateTimestamp = lastUpdateTimestamp;
    this.jobAuthorization = jobAuthorization;
    this.failureReason = failureReason;
    this.userTimeZone = userTimeZone;
    this.userLocale = userLocale;
    this.transferMode = transferMode;
  }

  @JsonProperty("state")
  @Override
  public PortabilityJob.State state() {
    return state;
  }

  @JsonProperty("exportService")
  @Override
  public String exportService() {
    return exportService;
  }

  @JsonProperty("importService")
  @Override
  public String importService() {
    return importService;
  }

  @JsonProperty("transferDataType")
  @Override
  public DataVertical transferDataType() {
    return transferDataType;
  }

  @JsonProperty("exportInformation")
  @Nullable
  @Override
  public String exportInformation() {
    return exportInformation;
  }

  @JsonProperty("createdTimestamp")
  @Override
  public Instant createdTimestamp() {
    return createdTimestamp;
  }

  @JsonProperty("lastUpdateTimestamp")
  @Override
  public Instant lastUpdateTimestamp() {
    return lastUpdateTimestamp;
  }

  @JsonProperty("jobAuthorization")
  @Override
  public JobAuthorization jobAuthorization() {
    return jobAuthorization;
  }

  @JsonProperty("failureReason")
  @Nullable
  @Override
  public String failureReason() {
    return failureReason;
  }

  @JsonProperty("userTimeZone")
  @Nullable
  @Override
  public TimeZone userTimeZone() {
    return userTimeZone;
  }

  @JsonProperty("userLocale")
  @Nullable
  @Override
  public String userLocale() {
    return userLocale;
  }

  @JsonProperty("transferMode")
  @Nullable
  @Override
  public PortabilityJob.TransferMode transferMode() {
    return transferMode;
  }

  @Override
  public String toString() {
    return "PortabilityJob{"
        + "state=" + state + ", "
        + "exportService=" + exportService + ", "
        + "importService=" + importService + ", "
        + "transferDataType=" + transferDataType + ", "
        + "exportInformation=" + exportInformation + ", "
        + "createdTimestamp=" + createdTimestamp + ", "
        + "lastUpdateTimestamp=" + lastUpdateTimestamp + ", "
        + "jobAuthorization=" + jobAuthorization + ", "
        + "failureReason=" + failureReason + ", "
        + "userTimeZone=" + userTimeZone + ", "
        + "userLocale=" + userLocale + ", "
        + "transferMode=" + transferMode
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PortabilityJob) {
      PortabilityJob that = (PortabilityJob) o;
      return this.state.equals(that.state())
          && this.exportService.equals(that.exportService())
          && this.importService.equals(that.importService())
          && this.transferDataType.equals(that.transferDataType())
          && (this.exportInformation == null ? that.exportInformation() == null : this.exportInformation.equals(that.exportInformation()))
          && this.createdTimestamp.equals(that.createdTimestamp())
          && this.lastUpdateTimestamp.equals(that.lastUpdateTimestamp())
          && this.jobAuthorization.equals(that.jobAuthorization())
          && (this.failureReason == null ? that.failureReason() == null : this.failureReason.equals(that.failureReason()))
          && (this.userTimeZone == null ? that.userTimeZone() == null : this.userTimeZone.equals(that.userTimeZone()))
          && (this.userLocale == null ? that.userLocale() == null : this.userLocale.equals(that.userLocale()))
          && (this.transferMode == null ? that.transferMode() == null : this.transferMode.equals(that.transferMode()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= state.hashCode();
    h$ *= 1000003;
    h$ ^= exportService.hashCode();
    h$ *= 1000003;
    h$ ^= importService.hashCode();
    h$ *= 1000003;
    h$ ^= transferDataType.hashCode();
    h$ *= 1000003;
    h$ ^= (exportInformation == null) ? 0 : exportInformation.hashCode();
    h$ *= 1000003;
    h$ ^= createdTimestamp.hashCode();
    h$ *= 1000003;
    h$ ^= lastUpdateTimestamp.hashCode();
    h$ *= 1000003;
    h$ ^= jobAuthorization.hashCode();
    h$ *= 1000003;
    h$ ^= (failureReason == null) ? 0 : failureReason.hashCode();
    h$ *= 1000003;
    h$ ^= (userTimeZone == null) ? 0 : userTimeZone.hashCode();
    h$ *= 1000003;
    h$ ^= (userLocale == null) ? 0 : userLocale.hashCode();
    h$ *= 1000003;
    h$ ^= (transferMode == null) ? 0 : transferMode.hashCode();
    return h$;
  }

  @Override
  public PortabilityJob.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends PortabilityJob.Builder {
    private PortabilityJob.State state;
    private String exportService;
    private String importService;
    private DataVertical transferDataType;
    private String exportInformation;
    private Instant createdTimestamp;
    private Instant lastUpdateTimestamp;
    private JobAuthorization jobAuthorization;
    private String failureReason;
    private TimeZone userTimeZone;
    private String userLocale;
    private PortabilityJob.TransferMode transferMode;
    Builder() {
    }
    private Builder(PortabilityJob source) {
      this.state = source.state();
      this.exportService = source.exportService();
      this.importService = source.importService();
      this.transferDataType = source.transferDataType();
      this.exportInformation = source.exportInformation();
      this.createdTimestamp = source.createdTimestamp();
      this.lastUpdateTimestamp = source.lastUpdateTimestamp();
      this.jobAuthorization = source.jobAuthorization();
      this.failureReason = source.failureReason();
      this.userTimeZone = source.userTimeZone();
      this.userLocale = source.userLocale();
      this.transferMode = source.transferMode();
    }
    @Override
    public PortabilityJob.Builder setState(PortabilityJob.State state) {
      if (state == null) {
        throw new NullPointerException("Null state");
      }
      this.state = state;
      return this;
    }
    @Override
    public PortabilityJob.Builder setExportService(String exportService) {
      if (exportService == null) {
        throw new NullPointerException("Null exportService");
      }
      this.exportService = exportService;
      return this;
    }
    @Override
    public PortabilityJob.Builder setImportService(String importService) {
      if (importService == null) {
        throw new NullPointerException("Null importService");
      }
      this.importService = importService;
      return this;
    }
    @Override
    public PortabilityJob.Builder setTransferDataType(DataVertical transferDataType) {
      if (transferDataType == null) {
        throw new NullPointerException("Null transferDataType");
      }
      this.transferDataType = transferDataType;
      return this;
    }
    @Override
    public PortabilityJob.Builder setExportInformation(String exportInformation) {
      this.exportInformation = exportInformation;
      return this;
    }
    @Override
    public PortabilityJob.Builder setCreatedTimestamp(Instant createdTimestamp) {
      if (createdTimestamp == null) {
        throw new NullPointerException("Null createdTimestamp");
      }
      this.createdTimestamp = createdTimestamp;
      return this;
    }
    @Override
    public PortabilityJob.Builder setLastUpdateTimestamp(Instant lastUpdateTimestamp) {
      if (lastUpdateTimestamp == null) {
        throw new NullPointerException("Null lastUpdateTimestamp");
      }
      this.lastUpdateTimestamp = lastUpdateTimestamp;
      return this;
    }
    @Override
    protected PortabilityJob.Builder setJobAuthorization(JobAuthorization jobAuthorization) {
      if (jobAuthorization == null) {
        throw new NullPointerException("Null jobAuthorization");
      }
      this.jobAuthorization = jobAuthorization;
      return this;
    }
    @Override
    public PortabilityJob.Builder setFailureReason(String failureReason) {
      this.failureReason = failureReason;
      return this;
    }
    @Override
    public PortabilityJob.Builder setUserTimeZone(TimeZone userTimeZone) {
      this.userTimeZone = userTimeZone;
      return this;
    }
    @Override
    public PortabilityJob.Builder setUserLocale(String userLocale) {
      this.userLocale = userLocale;
      return this;
    }
    @Override
    public PortabilityJob.Builder setTransferMode(PortabilityJob.TransferMode transferMode) {
      this.transferMode = transferMode;
      return this;
    }
    @Override
    public PortabilityJob build() {
      if (this.state == null
          || this.exportService == null
          || this.importService == null
          || this.transferDataType == null
          || this.createdTimestamp == null
          || this.lastUpdateTimestamp == null
          || this.jobAuthorization == null) {
        StringBuilder missing = new StringBuilder();
        if (this.state == null) {
          missing.append(" state");
        }
        if (this.exportService == null) {
          missing.append(" exportService");
        }
        if (this.importService == null) {
          missing.append(" importService");
        }
        if (this.transferDataType == null) {
          missing.append(" transferDataType");
        }
        if (this.createdTimestamp == null) {
          missing.append(" createdTimestamp");
        }
        if (this.lastUpdateTimestamp == null) {
          missing.append(" lastUpdateTimestamp");
        }
        if (this.jobAuthorization == null) {
          missing.append(" jobAuthorization");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_PortabilityJob(
          this.state,
          this.exportService,
          this.importService,
          this.transferDataType,
          this.exportInformation,
          this.createdTimestamp,
          this.lastUpdateTimestamp,
          this.jobAuthorization,
          this.failureReason,
          this.userTimeZone,
          this.userLocale,
          this.transferMode);
    }
  }

}
