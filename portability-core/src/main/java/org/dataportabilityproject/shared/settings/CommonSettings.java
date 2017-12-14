package org.dataportabilityproject.shared.settings;

import static org.dataportabilityproject.cloud.SupportedCloud.GOOGLE;
import static org.dataportabilityproject.shared.Config.Environment.LOCAL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.shared.Config.Environment;

/**
 * Common settings across multiple servers.
 */
public class CommonSettings {
  // The deployment environment. Can be LOCAL, TEST, QA, or PROD.
  private Environment env;
  // Which cloud to use. Can be LOCAL for in memory or GOOGLE for Google Cloud.
  private SupportedCloud cloud;
  private Boolean encryptedFlow;

  @JsonCreator
  public CommonSettings(
      @JsonProperty(value="env", required=true) Environment env,
      @JsonProperty(value="cloud", required=true) SupportedCloud cloud,
      @JsonProperty(value="encryptedFlow") Boolean encryptedFlow) {
    this.env = env;
    this.cloud = cloud;
    this.encryptedFlow = encryptedFlow;
    if (env == LOCAL && cloud == GOOGLE) {
      // This is a crude check to make sure we are only pointing to test projects when running
      // locally and connecting to GCP
      String googleProjectId = System.getenv("GOOGLE_PROJECT_ID");
      Preconditions.checkArgument(
          googleProjectId.endsWith("-local") || googleProjectId.endsWith("-test"),
          "Invalid project to connect to with env=LOCAL. " + googleProjectId + " doesn't appear to"
              + " be a local/test project since it doesn't end in -local or -test. Aborting");
    }
  }

  public Environment getEnv() {
    return env;
  }

  public SupportedCloud getCloud() {
    return cloud;
  }

  public Boolean getEncryptedFlow() {
    return encryptedFlow != null ? encryptedFlow : false;
  }
}
