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
import org.dataportabilityproject.config.ConfigUtils;

/**
 * Class to parse required API settings (see {@code ApiSettingsExtension}) from YAML config
 * files on the classpath.
 */
public final class YamlApiSettings {
  private static final String API_SETTINGS_PATH = "config/api.yaml";
  private static final String ENV_API_SETTINGS_PATH = "config/env/api.yaml";

  // TODO(rtannenbaum): Change these to URL types instead of String
  // Base url for all calls within the application
  private final String baseUrl;
  // Base url for direct to api calls within the application
  private final String baseApiUrl;

  @JsonCreator
  private YamlApiSettings(
      @JsonProperty(value = "baseUrl", required = true) String baseUrl,
      @JsonProperty(value = "baseApiUrl", required = true) String baseApiUrl) {
    this.baseUrl = baseUrl;
    this.baseApiUrl = baseApiUrl;
  }

  @JsonProperty
  public String baseUrl() {
    return baseUrl;
  }

  @JsonProperty
  public String baseApiUrl() {
    return baseApiUrl;
  }

  public static YamlApiSettings parse() throws IOException {
    // Any setting in both a base and env config will be overridden by the env definition.
    // This is enforced by the ordering of the settings files in the list below.
    ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
        .add(API_SETTINGS_PATH)
        .add(ENV_API_SETTINGS_PATH)
        .build();
    return parse(settingsFiles);
  }

  @VisibleForTesting
  static YamlApiSettings parse(ImmutableList<String> settingsFiles) throws IOException {
    InputStream combinedInputStream = ConfigUtils.getCombinedInputStream(settingsFiles);
    return parse(combinedInputStream);
  }

  private static YamlApiSettings parse(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(inputStream, YamlApiSettings.class);
  }

  // Prevent instantiation.
  private YamlApiSettings() {
    this.baseUrl = null;
    this.baseApiUrl = null;
  }
}
