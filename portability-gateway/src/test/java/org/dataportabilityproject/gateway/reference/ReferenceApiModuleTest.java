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
public class ReferenceApiModuleTest {
  private static String SETTINGS1_FILE = "test-settings1.yaml"; // baseUrl: foo
  private static String SETTINGS2_FILE = "test-settings2.yaml"; // baseApiUrl: bar
  private static String SETTINGS3_FILE = "test-settings3.yaml"; // unrecognizedFlag: baz
  private static String SETTINGS4_FILE = "test-settings4.yaml"; // baseUrl: baz

  @Test
  public void getApiSettings() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(SETTINGS1_FILE, SETTINGS2_FILE);
    ApiSettings apiSettings = ReferenceApiModule.getApiSettings(settingsFiles);
    assertThat(apiSettings.getBaseUrl()).isEqualTo("foo");
    assertThat(apiSettings.getBaseApiUrl()).isEqualTo("bar");
  }

  @Test
  public void getApiSettings_failOnUnrecognizedFlag() {
    ImmutableList<String> settingsFiles =
        ImmutableList.of(SETTINGS1_FILE, SETTINGS2_FILE, SETTINGS3_FILE);
    // Unrecognized field "unrecognizedFlag"
    assertThrows(UnrecognizedPropertyException.class,
        () -> ReferenceApiModule.getApiSettings(settingsFiles));
  }

  @Test
  public void getApiSettings_failOnMissingFlag() throws IOException {
    ImmutableList<String> settingsFiles = ImmutableList.of(SETTINGS1_FILE);
    // Missing required creator property 'baseApiUrl'
    assertThrows(MismatchedInputException.class,
        () -> ReferenceApiModule.getApiSettings(settingsFiles));
  }

  @Test
  public void getApiSettings_override() throws IOException {
    ImmutableList<String> settingsFiles =
        ImmutableList.of(SETTINGS1_FILE, SETTINGS2_FILE, SETTINGS4_FILE);
    ApiSettings apiSettings = ReferenceApiModule.getApiSettings(settingsFiles);
    assertThat(apiSettings.getBaseUrl()).isEqualTo("baz");
    assertThat(apiSettings.getBaseApiUrl()).isEqualTo("bar");
  }
}
