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

package org.dataportabilityproject.config.yaml.parse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import org.dataportabilityproject.api.launcher.Constants.Environment;
import org.dataportabilityproject.config.ConfigUtils;

/**
 * Class to parse required common settings (see {@code SettingsExtension}) from YAML config
 * files on the classpath.
 *
 * <p>Distributions must configure API settings in config/common.yaml and (optional)
 * config/env/common.yaml. See distributions/demo-server as an example.
 *
 * <p>The env/ file is an optional mechanism to allow environment-specific overrides of settings in
 * the base common.yaml. This requires a distribution's build.gradle to copy the common.yaml file
 * for the appropriate environment into config/env/common.yaml in the jar. TODO(#202): Do this for
 * the sample Google deployments as a reference.
 */
public final class YamlCommonSettings {
  private static final String COMMON_SETTINGS_PATH = "config/common.yaml";
  private static final String ENV_COMMON_SETTINGS_PATH = "config/env/common.yaml";

  private final String cloud;
  private final Environment environment;

  @JsonCreator
  private YamlCommonSettings(
      @JsonProperty(value = "cloud", required = true) String cloud,
      @JsonProperty(value = "environment", required = true) Environment environment) {
    this.cloud = cloud;
    this.environment = environment;
  }

  @JsonProperty
  public String cloud() {
    return cloud;
  }

  @JsonProperty
  public Environment environment() {
    return environment;
  }

  public static YamlCommonSettings parse() throws IOException {
    // Any setting in both a base and env config will be overridden by the env definition.
    // This is enforced by the ordering of the settings files in the list below.
    ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
        .add(COMMON_SETTINGS_PATH)
        .add(ENV_COMMON_SETTINGS_PATH)
        .build();
    return parse(settingsFiles);
  }

  @VisibleForTesting
  static YamlCommonSettings parse(ImmutableList<String> settingsFiles) throws IOException {
    InputStream combinedInputStream = ConfigUtils.getCombinedInputStream(settingsFiles);
    return parse(combinedInputStream);
  }

  private static YamlCommonSettings parse(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(inputStream, YamlCommonSettings.class);
  }

  // prevent instantiation. Use static getters to retrieve fields.
  private YamlCommonSettings() {
    cloud = null;
    environment = null;
  }
}
