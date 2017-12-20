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
package org.dataportabilityproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.shared.Config.Environment;
import org.dataportabilityproject.shared.settings.CommonSettings;

/**
 * A class that contains all flags passed from the commandline.
 */
public class PortabilityFlags {
  private static PortabilityFlags INSTANCE = null;
  private final CommonSettings commonSettings;

  private PortabilityFlags(CommonSettings commonSettings) {
    this.commonSettings = commonSettings;
  }

  /**
   * Initialize the PortabilityFlags global configuration parameters from provided command line.
   */
  public static void parse() {
    if (INSTANCE != null) {
      throw new IllegalStateException("Trying to initialize flags a second time");
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      InputStream in =
          PortabilityFlags.class.getClassLoader().getResourceAsStream("settings/common.yaml");
      CommonSettings commonSettings = mapper.readValue(in, CommonSettings.class);
      INSTANCE = new PortabilityFlags(commonSettings);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing common settings", e);
    }
  }

  public static SupportedCloud cloud() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'cloud' before flags have been initialized");
    return INSTANCE.commonSettings.getCloud();
  }

  public static Environment environment() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'environment' before flags have been initialized");
    return INSTANCE.commonSettings.getEnv();
  }

  public static boolean encryptedFlow() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'encryptedFlow' before flags have been initialized");
    return INSTANCE.commonSettings.getEncryptedFlow();
  }

  public static ImmutableList<String> supportedServiceProviders() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'supportedServiceProviders' before flags have been initialized");
    return INSTANCE.commonSettings.getServiceProviderClasses();
  }
}
