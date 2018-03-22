/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings used in both the API and worker servers.
 *
 * These settings are parsed from config/common.yaml and config/env/common.yaml.
 *
 * <p>The env/ file is an optional mechanism to allow environment-specific overrides of settings in
 * the base common.yaml. This requires a distribution's build.gradle to copy the common.yaml file
 * for the appropriate environment into config/env/common.yaml in the jar. TODO(#202): Do this for
 * the sample Google deployments as a reference.
 */
public class CommonSettings {
  public enum Environment {
    LOCAL,
    TEST,
    QA,
    PROD
  }

  private static final Logger logger = LoggerFactory.getLogger(CommonSettings.class);
  /**
   * We use a static singleton to facilitate:
   * 1. core libraries injecting a CommonSettings (provided in {@code CommonSettingsModule})
   * 2. extensions, which do not necessarily use Guice, retrieving the settings statically here
   */
  private static CommonSettings commonSettings;

  private static final String COMMON_SETTINGS_PATH = "config/env/common.yaml";
  private static final String ENV_COMMON_SETTINGS_PATH = "config/env/common.yaml";

  private final String cloud;
  private final Environment environment;

  public CommonSettings() {
    cloud = null;
    environment = null;
  }

  @JsonCreator
  public CommonSettings(
      @JsonProperty(value = "cloud", required = true) String cloud,
      @JsonProperty(value = "environment", required = true) Environment environment) {
    this.cloud = cloud;
    this.environment = environment;
  }

  public synchronized static CommonSettings getCommonSettings() {
    if (commonSettings != null) {
      return commonSettings;
    }

    // Any setting in both a base and env config will be overridden by the env definition.
    // This is enforced by the ordering of the settings files in the list below.
    try {
      ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
          .add(COMMON_SETTINGS_PATH)
          .add(ENV_COMMON_SETTINGS_PATH)
          .build();
      CommonSettings tempCommonSettings = getCommonSettings(settingsFiles);
      logger.debug("Parsed flags: {}", tempCommonSettings);
      commonSettings = tempCommonSettings;
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing common settings", e);
    }
    return commonSettings;
  }

  public String getCloud() {
    return cloud;
  }

  public Environment getEnvironment() {
    return environment;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("cloud", cloud)
        .add("environment", environment)
        .toString();
  }

  private static CommonSettings getCommonSettings(ImmutableList<String> settingsFiles)
      throws IOException {
    InputStream combinedInputStream = ConfigUtils.getSettingsCombinedInputStream(settingsFiles);
    return getCommonSettings(combinedInputStream);
  }

  private static CommonSettings getCommonSettings(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(inputStream, CommonSettings.class);
  }
}
