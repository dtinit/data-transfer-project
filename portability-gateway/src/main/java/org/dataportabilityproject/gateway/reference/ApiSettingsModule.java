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

package org.dataportabilityproject.gateway.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import org.dataportabilityproject.config.ConfigUtils;
import org.dataportabilityproject.gateway.ApiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides {@link ApiSettings} parsed from config/api.yaml and config/env/api.yaml.
 *
 * <p>The env/ file is an optional mechanism to allow environment-specific overrides of settings in
 * the base api.yaml. This requires a distribution's build.gradle to copy the api.yaml file for the
 * appropriate environment into config/env/api.yaml in the jar.
 * TODO(#202): Do this for the sample Google deployments as a reference. Consider using 'projectId'
 * as an example Google custom yaml setting.
 */
class ApiSettingsModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(ApiSettingsModule.class);
  private static final String API_SETTINGS_PATH = "config/api.yaml";
  private static final String ENV_API_SETTINGS_PATH = "config/env/api.yaml";

  @Override
  public void configure() {}

  @Provides
  @Singleton
  ApiSettings getApiSettings() {
    // Any setting in both a base and env config will be overridden by the env definition.
    // This is enforced by the ordering of the settings files in the list below.
    try {
      ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
          .add(API_SETTINGS_PATH)
          .add(ENV_API_SETTINGS_PATH)
          .build();
      ApiSettings apiSettings = getApiSettings(settingsFiles);
      logger.debug("Parsed flags: {}", apiSettings);
      return apiSettings;
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing api settings", e);
    }
  }

  static ApiSettings getApiSettings(ImmutableList<String> settingsFiles) throws IOException {
    InputStream combinedInputStream = ConfigUtils.getSettingsCombinedInputStream(settingsFiles);
    return getApiSettings(combinedInputStream);
  }

  static ApiSettings getApiSettings(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(inputStream, ApiSettings.class);
  }
}
