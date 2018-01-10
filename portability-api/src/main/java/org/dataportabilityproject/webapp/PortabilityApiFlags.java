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
package org.dataportabilityproject.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import org.dataportabilityproject.shared.settings.ApiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that contains all flags exlusive to the API server.
 */
public class PortabilityApiFlags {
  private static final Logger logger = LoggerFactory.getLogger(PortabilityApiFlags.class);
  private static PortabilityApiFlags INSTANCE = null;
  private final ApiSettings apiSettings;

  private PortabilityApiFlags(ApiSettings apiSettings) {
    this.apiSettings = apiSettings;
  }

  /**
   * Initialize PortabilityApiFlags global configuration parameters from provided command line.
   */
  public static void parse() {
    if (INSTANCE != null) {
      throw new IllegalStateException("Trying to initialize flags a second time");
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      InputStream in =
          PortabilityApiFlags.class.getClassLoader().getResourceAsStream("settings/api.yaml");
      ApiSettings apiSettings = mapper.readValue(in, ApiSettings.class);
      INSTANCE = new PortabilityApiFlags(apiSettings);
      logger.debug("Parsed flags: {}", apiSettings);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing api settings", e);
    }
  }

  public static String baseUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseUrl' before flags have been initialized");
    return INSTANCE.apiSettings.getBaseUrl();
  }

  public static String baseApiUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseApiUrl' before flags have been initialized");
    return INSTANCE.apiSettings.getBaseApiUrl();
  }
}
