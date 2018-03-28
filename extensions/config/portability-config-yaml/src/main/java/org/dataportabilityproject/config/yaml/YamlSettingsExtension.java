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

package org.dataportabilityproject.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.api.launcher.CommonSettings;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.extension.SettingsExtension;
import org.dataportabilityproject.config.ConfigUtils;

/**
 * {@link SettingsExtension} that parses configuration from YAML files on the classpath.
 *
 * <p>{@code CommonSettings} are parsed from config/common.yaml and config/env/common.yaml.
 * Distributions must configure common settings. See distributions/demo-server as an example.
 *
 * <p>The env/ file is an optional mechanism to allow environment-specific overrides of settings in
 * the base common.yaml. This requires a distribution's build.gradle to copy the common.yaml file
 * for the appropriate environment into config/env/common.yaml in the jar. TODO(#202): Do this for
 * the sample Google deployments as a reference.
 *
 * <p>Custom, extension-specific settings are parsed from config/custom.yaml.
 */
public class YamlSettingsExtension implements SettingsExtension {
  // YAML files where common settings are stored. Distributions must include these on the
  // classpath. See distributions/demo-server for an example.
  private static final String COMMON_SETTINGS_PATH = "config/common.yaml";
  private static final String ENV_COMMON_SETTINGS_PATH = "config/env/common.yaml";

  // YAML file where custom extension settings may be configured.
  private static final String CUSTOM_SETTINGS_PATH = "config/custom.yaml";

  private CommonSettings commonSettings;
  private Map<String, Object> customSettings;


  @Override
  public CommonSettings getCommonSettings() {
    return commonSettings;
  }

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    if (customSettings.containsKey(setting)) {
      return (T) customSettings.get(setting);
    }
    return defaultValue;
  }

  @Override
  public void initialize(ExtensionContext context) {
    initCommonSettings();
    try {
      initCustomSettings();
    } catch (IOException e) {
      throw new RuntimeException("Could not initialize custom settings", e);
    }
  }

  @VisibleForTesting
  static Map<String, Object> getCustomSettings(InputStream in)
      throws IOException {
    if (in == null) {
      return new HashMap<>();
    }
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(in, Map.class);
  }

  private void initCommonSettings() {
    // Any setting in both a base and env config will be overridden by the env definition.
    // This is enforced by the ordering of the settings files in the list below.
    try {
      ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
          .add(COMMON_SETTINGS_PATH)
          .add(ENV_COMMON_SETTINGS_PATH)
          .build();
      CommonSettings tempCommonSettings = getCommonSettings(settingsFiles);
      commonSettings = tempCommonSettings;
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing common settings", e);
    }
  }

  /**
   * Parses an input stream to an extension's custom yaml configuration into a generic
   * Map<String, Object>.
   */
  private void initCustomSettings() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(CUSTOM_SETTINGS_PATH);
    InputStream in = ConfigUtils.getCombinedInputStream(settingsFiles);
    customSettings = getCustomSettings(in);
  }

  private static CommonSettings getCommonSettings(ImmutableList<String> settingsFiles)
      throws IOException {
    InputStream combinedInputStream = ConfigUtils.getCombinedInputStream(settingsFiles);
    return getCommonSettings(combinedInputStream);
  }

  private static CommonSettings getCommonSettings(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(inputStream, CommonSettings.class);
  }
}
