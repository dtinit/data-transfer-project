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

package org.datatransferproject.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.config.ConfigUtils;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link SettingsExtension} that reads configuration from YAML files on the classpath.
 */
public class YamlSettingsExtension implements SettingsExtension {

  // YAML file where custom extension settings may be configured.
  private static final String EXTENSION_SETTINGS_PATH = "config/extension.yaml";

  // YAML files where common settings must be configured. env/common.yaml is an optional mechanism
  // to override base configuration with environment-specific values.
  private static final String COMMON_SETTINGS_PATH = "config/common.yaml";
  private static final String ENV_COMMON_SETTINGS_PATH = "config/env/common.yaml";

  // YAML files where API settings must be configured. env/api.yaml is an optional mechanism
  // to override base configuration with environment-specific values.
  private static final String API_SETTINGS_PATH = "config/api.yaml";
  private static final String ENV_API_SETTINGS_PATH = "config/env/api.yaml";

  // YAML files where retry settings must be configured.
  // TODO: support wildcard expansion of file names
  private static final String RETRY_LIBRARY_PATH = "config/retry/default.yaml";

  private static final String TRANSFER_SETTINGS_PATH = "config/transfer.yaml";
  private static final String ENV_TRANSFER_SETTINGS_PATH = "config/env/transfer.yaml";

  private Map<String, Object> settings;

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    if (settings.containsKey(setting)) {
      return (T) settings.get(setting);
    }
    return defaultValue;
  }

  @Override
  public void initialize() {
    parseSimple(getSimpleInputStream());
    parseRetryLibrary(getRetryLibraryStream());
  }

  @VisibleForTesting
  void parseSimple(InputStream in) {
    if (in == null) {
      settings = new HashMap<>();
    } else {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        settings = mapper.readValue(in, Map.class);
      } catch (IOException e) {
        throw new RuntimeException("Could not parse extension settings", e);
      }
    }
  }

  @VisibleForTesting
  void parseRetryLibrary(InputStream in) {
    if (in != null) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        settings.put("retryLibrary", mapper.readValue(in, RetryStrategyLibrary.class));
      } catch (IOException e) {
        throw new RuntimeException("Could not parse extension settings", e);
      }
    }
  }

  private InputStream getSimpleInputStream() {
    ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
        .add(COMMON_SETTINGS_PATH)
        .add(ENV_COMMON_SETTINGS_PATH)
        .add(API_SETTINGS_PATH)
        .add(ENV_API_SETTINGS_PATH)
        .add(EXTENSION_SETTINGS_PATH)
        .add(TRANSFER_SETTINGS_PATH)
        .add(ENV_TRANSFER_SETTINGS_PATH)
        .build();
    return ConfigUtils.getCombinedInputStream(settingsFiles);
  }

  private InputStream getRetryLibraryStream() {
    // TODO: read from extensions-specific libraries here
    return ConfigUtils.getCombinedInputStream(ImmutableList.of(RETRY_LIBRARY_PATH));
  }
}
