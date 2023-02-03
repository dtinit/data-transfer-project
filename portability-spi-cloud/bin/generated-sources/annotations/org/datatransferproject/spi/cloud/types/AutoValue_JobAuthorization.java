package org.datatransferproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_JobAuthorization extends JobAuthorization {

  private final JobAuthorization.State state;

  private final String encryptedInitialExportAuthData;

  private final String encryptedInitialImportAuthData;

  private final String encryptedAuthData;

  private final String encryptionScheme;

  private final String sessionSecretKey;

  private final String authPublicKey;

  private final String instanceId;

  private AutoValue_JobAuthorization(
      JobAuthorization.State state,
      @Nullable String encryptedInitialExportAuthData,
      @Nullable String encryptedInitialImportAuthData,
      @Nullable String encryptedAuthData,
      @Nullable String encryptionScheme,
      @Nullable String sessionSecretKey,
      @Nullable String authPublicKey,
      @Nullable String instanceId) {
    this.state = state;
    this.encryptedInitialExportAuthData = encryptedInitialExportAuthData;
    this.encryptedInitialImportAuthData = encryptedInitialImportAuthData;
    this.encryptedAuthData = encryptedAuthData;
    this.encryptionScheme = encryptionScheme;
    this.sessionSecretKey = sessionSecretKey;
    this.authPublicKey = authPublicKey;
    this.instanceId = instanceId;
  }

  @JsonProperty("state")
  @Override
  public JobAuthorization.State state() {
    return state;
  }

  @JsonProperty("encryptedInitialExportAuthData")
  @Nullable
  @Override
  public String encryptedInitialExportAuthData() {
    return encryptedInitialExportAuthData;
  }

  @JsonProperty("encryptedInitialImportAuthData")
  @Nullable
  @Override
  public String encryptedInitialImportAuthData() {
    return encryptedInitialImportAuthData;
  }

  @JsonProperty("encryptedAuthData")
  @Nullable
  @Override
  public String encryptedAuthData() {
    return encryptedAuthData;
  }

  @JsonProperty("encryptionScheme")
  @Nullable
  @Override
  public String encryptionScheme() {
    return encryptionScheme;
  }

  @JsonProperty("sessionSecretKey")
  @Deprecated
  @Nullable
  @Override
  public String sessionSecretKey() {
    return sessionSecretKey;
  }

  @JsonProperty("authPublicKey")
  @Nullable
  @Override
  public String authPublicKey() {
    return authPublicKey;
  }

  @JsonProperty("instanceId")
  @Nullable
  @Override
  public String instanceId() {
    return instanceId;
  }

  @Override
  public String toString() {
    return "JobAuthorization{"
        + "state=" + state + ", "
        + "encryptedInitialExportAuthData=" + encryptedInitialExportAuthData + ", "
        + "encryptedInitialImportAuthData=" + encryptedInitialImportAuthData + ", "
        + "encryptedAuthData=" + encryptedAuthData + ", "
        + "encryptionScheme=" + encryptionScheme + ", "
        + "sessionSecretKey=" + sessionSecretKey + ", "
        + "authPublicKey=" + authPublicKey + ", "
        + "instanceId=" + instanceId
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof JobAuthorization) {
      JobAuthorization that = (JobAuthorization) o;
      return this.state.equals(that.state())
          && (this.encryptedInitialExportAuthData == null ? that.encryptedInitialExportAuthData() == null : this.encryptedInitialExportAuthData.equals(that.encryptedInitialExportAuthData()))
          && (this.encryptedInitialImportAuthData == null ? that.encryptedInitialImportAuthData() == null : this.encryptedInitialImportAuthData.equals(that.encryptedInitialImportAuthData()))
          && (this.encryptedAuthData == null ? that.encryptedAuthData() == null : this.encryptedAuthData.equals(that.encryptedAuthData()))
          && (this.encryptionScheme == null ? that.encryptionScheme() == null : this.encryptionScheme.equals(that.encryptionScheme()))
          && (this.sessionSecretKey == null ? that.sessionSecretKey() == null : this.sessionSecretKey.equals(that.sessionSecretKey()))
          && (this.authPublicKey == null ? that.authPublicKey() == null : this.authPublicKey.equals(that.authPublicKey()))
          && (this.instanceId == null ? that.instanceId() == null : this.instanceId.equals(that.instanceId()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= state.hashCode();
    h$ *= 1000003;
    h$ ^= (encryptedInitialExportAuthData == null) ? 0 : encryptedInitialExportAuthData.hashCode();
    h$ *= 1000003;
    h$ ^= (encryptedInitialImportAuthData == null) ? 0 : encryptedInitialImportAuthData.hashCode();
    h$ *= 1000003;
    h$ ^= (encryptedAuthData == null) ? 0 : encryptedAuthData.hashCode();
    h$ *= 1000003;
    h$ ^= (encryptionScheme == null) ? 0 : encryptionScheme.hashCode();
    h$ *= 1000003;
    h$ ^= (sessionSecretKey == null) ? 0 : sessionSecretKey.hashCode();
    h$ *= 1000003;
    h$ ^= (authPublicKey == null) ? 0 : authPublicKey.hashCode();
    h$ *= 1000003;
    h$ ^= (instanceId == null) ? 0 : instanceId.hashCode();
    return h$;
  }

  @Override
  public JobAuthorization.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends JobAuthorization.Builder {
    private JobAuthorization.State state;
    private String encryptedInitialExportAuthData;
    private String encryptedInitialImportAuthData;
    private String encryptedAuthData;
    private String encryptionScheme;
    private String sessionSecretKey;
    private String authPublicKey;
    private String instanceId;
    Builder() {
    }
    private Builder(JobAuthorization source) {
      this.state = source.state();
      this.encryptedInitialExportAuthData = source.encryptedInitialExportAuthData();
      this.encryptedInitialImportAuthData = source.encryptedInitialImportAuthData();
      this.encryptedAuthData = source.encryptedAuthData();
      this.encryptionScheme = source.encryptionScheme();
      this.sessionSecretKey = source.sessionSecretKey();
      this.authPublicKey = source.authPublicKey();
      this.instanceId = source.instanceId();
    }
    @Override
    public JobAuthorization.Builder setState(JobAuthorization.State state) {
      if (state == null) {
        throw new NullPointerException("Null state");
      }
      this.state = state;
      return this;
    }
    @Override
    public JobAuthorization.Builder setEncryptedInitialExportAuthData(String encryptedInitialExportAuthData) {
      this.encryptedInitialExportAuthData = encryptedInitialExportAuthData;
      return this;
    }
    @Override
    public JobAuthorization.Builder setEncryptedInitialImportAuthData(String encryptedInitialImportAuthData) {
      this.encryptedInitialImportAuthData = encryptedInitialImportAuthData;
      return this;
    }
    @Override
    public JobAuthorization.Builder setEncryptedAuthData(String encryptedAuthData) {
      this.encryptedAuthData = encryptedAuthData;
      return this;
    }
    @Override
    public JobAuthorization.Builder setEncryptionScheme(String encryptionScheme) {
      this.encryptionScheme = encryptionScheme;
      return this;
    }
    @Override
    public JobAuthorization.Builder setSessionSecretKey(String sessionSecretKey) {
      this.sessionSecretKey = sessionSecretKey;
      return this;
    }
    @Override
    public JobAuthorization.Builder setAuthPublicKey(String authPublicKey) {
      this.authPublicKey = authPublicKey;
      return this;
    }
    @Override
    public JobAuthorization.Builder setInstanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }
    @Override
    public JobAuthorization build() {
      if (this.state == null) {
        String missing = " state";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_JobAuthorization(
          this.state,
          this.encryptedInitialExportAuthData,
          this.encryptedInitialImportAuthData,
          this.encryptedAuthData,
          this.encryptionScheme,
          this.sessionSecretKey,
          this.authPublicKey,
          this.instanceId);
    }
  }

}
