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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.config.ConfigUtils;

/** Class to parse custom extension settings from a YAML config file on the classpath. */
public final class YamlExtensionSettings {
  // YAML file where custom extension settings may be configured.
  private static final String EXTENSION_SETTINGS_PATH = "config/extension.yaml";

  private Map<String, Object> extensionSettings;

  private YamlExtensionSettings(Map<String, Object> extensionSettings) {
    this.extensionSettings = extensionSettings;
  }

  public static YamlExtensionSettings parse() throws RuntimeException {
    ImmutableList<String> settingsFiles = ImmutableList.of(EXTENSION_SETTINGS_PATH);
    InputStream in = ConfigUtils.getCombinedInputStream(settingsFiles);
    return parse(in);
  }

  @VisibleForTesting
  static YamlExtensionSettings parse(InputStream in) {
    if (in == null) {
      return new YamlExtensionSettings(new HashMap<>());
    } else {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        return new YamlExtensionSettings(mapper.readValue(in, Map.class));
      } catch (IOException e) {
        throw new RuntimeException("Could not parse extension settings", e);
      }
    }
  }

  public <T> T getSetting(String setting, T defaultValue) {
    if (extensionSettings.containsKey(setting)) {
      return (T) extensionSettings.get(setting);
    }
    return defaultValue;
  }
}
