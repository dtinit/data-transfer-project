/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.shared.settings;

import static org.dataportabilityproject.cloud.SupportedCloud.GOOGLE;
import static org.dataportabilityproject.shared.Config.Environment.LOCAL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("env", env)
        .add("cloud", cloud)
        .add("encryptedFlow", encryptedFlow)
        .add("serviceProviderClasses", serviceProviderClasses)
        .toString();
  }
}
