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

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.config.yaml.parse.YamlApiSettings;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YamlApiSettingsTest {
  /**
   * baseUrl: https://localhost:3000
   * baseApiUrl: https://localhost:8080
   */
  private static String API_SETTINGS_1 = "api-1.yaml";
  /**
   * baseUrl: www.aBaseUrl.com
   */
  private static String API_SETTINGS_2 = "api-2.yaml";
  /**
   * baseApiUrl: www.aBaseApiUrl.com
   */
  private static String API_SETTINGS_3 = "api-3.yaml";
  /**
   * unrecognizedFlag: foo
   */
  private static String API_SETTINGS_4 = "api-4.yaml";

  @Test
  public void parse() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1);
    YamlApiSettings apiSettings = YamlApiSettings.parse(settingsFiles);
    assertThat(apiSettings.baseUrl()).isEqualTo("https://localhost:3000");
    assertThat(apiSettings.baseApiUrl()).isEqualTo("https://localhost:8080");
  }

  @Test
  public void parse_failOnUnrecognizedFlag() {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1, API_SETTINGS_4);
      // Unrecognized field "unrecognizedFlag"
      Assertions.assertThrows(UnrecognizedPropertyException.class,
          () -> YamlApiSettings.parse(settingsFiles));
  }

  @Test
  public void parse_failOnMissingFlag() {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_3);
      // Missing required creator property 'baseApiUrl'
      Assertions.assertThrows(MismatchedInputException.class,
          () -> YamlApiSettings.parse(settingsFiles));
  }

  @Test
  public void parse_override() throws IOException {
    // www.aBaseUrl.com should override https://localhost:3000
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1, API_SETTINGS_2);
    YamlApiSettings apiSettings = YamlApiSettings.parse(settingsFiles);
    assertThat(apiSettings.baseUrl()).isEqualTo("www.aBaseUrl.com");

    // reorder settings files - now https://localhost:3000 should override www.aBaseUrl.com
    settingsFiles = ImmutableList.of(API_SETTINGS_2, API_SETTINGS_1);
    apiSettings = YamlApiSettings.parse(settingsFiles);
    assertThat(apiSettings.baseUrl()).isEqualTo("https://localhost:3000");
  }
}
