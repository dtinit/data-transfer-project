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

package org.dataportabilityproject.config.yaml.extension;

import java.io.IOException;
import org.dataportabilityproject.api.launcher.Constants.Environment;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.config.extension.ApiSettingsExtension;
import org.dataportabilityproject.config.extension.Flag;
import org.dataportabilityproject.config.yaml.parse.YamlApiSettings;
import org.dataportabilityproject.config.yaml.parse.YamlCommonSettings;
import org.dataportabilityproject.config.yaml.parse.YamlExtensionSettings;

/**
 * {@link ApiSettingsExtension} that parses configuration from YAML files on the classpath.
 *
 * <p>Distributions must configure API settings in config/api.yaml and (optional)
 * config/env/api.yaml. See distributions/demo-server as an example.
 *
 * <p>The env/ file is an optional mechanism to allow environment-specific overrides of settings in
 * the base api.yaml. This requires a distribution's build.gradle to copy the api.yaml file
 * for the appropriate environment into config/env/api.yaml in the jar. TODO(#202): Do this for
 * the sample Google deployments as a reference.
 */
public class YamlApiSettingsExtension implements ApiSettingsExtension {
  private YamlApiSettings apiSettings;
  private YamlCommonSettings commonSettings;
  private YamlExtensionSettings yamlExtensionSettings;

  @Override
  public void initialize(ExtensionContext context) {
    try {
      apiSettings = YamlApiSettings.parse();
      commonSettings = YamlCommonSettings.parse();
      yamlExtensionSettings = YamlExtensionSettings.parse();
    } catch (IOException e) {
      throw new RuntimeException("Problem parsing API flags", e);
    }
  }

  @Override
  @Flag
  public String baseUrl() {
    return apiSettings.baseUrl();
  }

  @Override
  @Flag
  public String baseApiUrl() {
    return apiSettings.baseApiUrl();
  }

  @Override
  @Flag
  public String cloud() {
    return commonSettings.cloud();
  }

  @Override
  @Flag
  public Environment environment() {
    return commonSettings.environment();
  }

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    return yamlExtensionSettings.getSetting(setting, defaultValue);
  }
}
