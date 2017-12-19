package org.dataportabilityproject.shared.settings;

import static org.dataportabilityproject.cloud.SupportedCloud.GOOGLE;
import static org.dataportabilityproject.shared.Config.Environment.LOCAL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.shared.Config.Environment;

/**
 * Common settings across multiple servers.
 */
public class CommonSettings {
  private static final String PROVIDER_PREFIX = "org.dataportabilityproject.serviceProviders";

  // The deployment environment. Can be LOCAL, TEST, QA, or PROD.
  private final Environment env;
  // Which cloud to use. Can be LOCAL for in memory or GOOGLE for Google Cloud.
  private final SupportedCloud cloud;
  private final String[] serviceProviderClasses;
  private final Boolean encryptedFlow;

  @JsonCreator
  public CommonSettings(
      @JsonProperty(value="env", required=true) Environment env,
      @JsonProperty(value="cloud", required=true) SupportedCloud cloud,
      @JsonProperty(value="serviceProviderClasses", required=true) String[] serviceProviderClasses,
      @JsonProperty(value="encryptedFlow") Boolean encryptedFlow) {
    this.env = env;
    this.cloud = cloud;
    for (String providerClass : serviceProviderClasses) {
      if (!providerClass.startsWith(PROVIDER_PREFIX)) {
        throw new IllegalArgumentException(providerClass + " must start with " + PROVIDER_PREFIX);
      }
    }
    this.serviceProviderClasses = serviceProviderClasses;
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

  public ImmutableList<String> getServiceProviderClasses() {
    return ImmutableList.copyOf(serviceProviderClasses);
  }

  public Boolean getEncryptedFlow() {
    return encryptedFlow != null ? encryptedFlow : false;
  }
}
