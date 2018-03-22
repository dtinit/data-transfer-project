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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.gateway.ApiSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiSettingsModuleTest {
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
  public void getApiSettings() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1);
    ApiSettings apiSettings = ApiSettingsModule.getApiSettings(settingsFiles);
    assertThat(apiSettings.getBaseUrl()).isEqualTo("https://localhost:3000");
    assertThat(apiSettings.getBaseApiUrl()).isEqualTo("https://localhost:8080");
  }

  @Test
  public void getApiSettings_failOnUnrecognizedFlag() {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1, API_SETTINGS_4);
    // Unrecognized field "unrecognizedFlag"
    assertThrows(UnrecognizedPropertyException.class,
        () -> ApiSettingsModule.getApiSettings(settingsFiles));
  }

  @Test
  public void getApiSettings_failOnMissingFlag() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_3);
    // Missing required creator property 'baseApiUrl'
    assertThrows(MismatchedInputException.class,
        () -> ApiSettingsModule.getApiSettings(settingsFiles));
  }

  @Test
  public void getApiSettings_override() throws IOException {
    // www.aBaseUrl.com should override https://localhost:3000
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1, API_SETTINGS_2);
    ApiSettings apiSettings = ApiSettingsModule.getApiSettings(settingsFiles);
    assertThat(apiSettings.getBaseUrl()).isEqualTo("www.aBaseUrl.com");

    // reorder settings files - now https://localhost:3000 should override www.aBaseUrl.com
    settingsFiles = ImmutableList.of(API_SETTINGS_2, API_SETTINGS_1);
    apiSettings = ApiSettingsModule.getApiSettings(settingsFiles);
    assertThat(apiSettings.getBaseUrl()).isEqualTo("https://localhost:3000");
  }
}
