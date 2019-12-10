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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import org.datatransferproject.config.ConfigUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YamlSettingsExtensionTest {
  /**
   * baseUrl: https://localhost:3000
   * baseApiUrl: https://localhost:8080
   */
  private static String API_SETTINGS_1 = "api-1.yaml";
  /**
   * baseUrl: www.aBaseUrl.com
   */
  private static String API_SETTINGS_2 = "api-2.yaml";

  @Test
  public void parse() {
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1);
    InputStream in = ConfigUtils.getCombinedInputStream(settingsFiles);
    YamlSettingsExtension settingsExtension = new YamlSettingsExtension();
    settingsExtension.parseSimple(in);
    assertThat((String) settingsExtension.getSetting("baseUrl", null))
        .isEqualTo("https://localhost:3000");
    assertThat((String) settingsExtension.getSetting("baseApiUrl", null))
        .isEqualTo("https://localhost:8080");
  }

  @Test
  public void parse_override() {
    // www.aBaseUrl.com should override https://localhost:3000
    ImmutableList<String> settingsFiles = ImmutableList.of(API_SETTINGS_1, API_SETTINGS_2);
    YamlSettingsExtension settingsExtension = new YamlSettingsExtension();
    InputStream in = ConfigUtils.getCombinedInputStream(settingsFiles);
    settingsExtension.parseSimple(in);
    assertThat((String) settingsExtension.getSetting("baseUrl", null))
        .isEqualTo("www.aBaseUrl.com");

    // reorder settings files - now https://localhost:3000 should override www.aBaseUrl.com
    settingsFiles = ImmutableList.of(API_SETTINGS_2, API_SETTINGS_1);
    in = ConfigUtils.getCombinedInputStream(settingsFiles);
    settingsExtension.parseSimple(in);
    assertThat((String) settingsExtension.getSetting("baseUrl", null))
        .isEqualTo("https://localhost:3000");
  }
}
