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

import java.io.IOException;
import org.dataportabilityproject.api.launcher.Constants.Environment;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.config.extension.Flag;
import org.dataportabilityproject.config.extension.WorkerSettingsExtension;

/**
 * {@link WorkerSettingsExtension} that parses configuration from YAML files on the classpath.
 */
public class YamlWorkerSettingsExtension implements WorkerSettingsExtension {
  private YamlCommonSettings commonSettings;
  private YamlExtensionSettings yamlExtensionSettings;

  @Override
  public void initialize(ExtensionContext context) {
    try {
      commonSettings = YamlCommonSettings.parse();
      yamlExtensionSettings = YamlExtensionSettings.parse();
    } catch (IOException e) {
      throw new RuntimeException("Problem parsing API flags", e);
    }
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
