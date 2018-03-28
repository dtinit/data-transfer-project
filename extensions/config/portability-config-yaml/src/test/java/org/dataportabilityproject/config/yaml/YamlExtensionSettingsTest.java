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

package org.dataportabilityproject.config.yaml;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YamlExtensionSettingsTest {

  @Test
  public void parse() {
    String testYaml = "foo: fooValue\nbar: barValue";
    InputStream in = new ByteArrayInputStream(testYaml.getBytes(StandardCharsets.UTF_8));
      YamlExtensionSettings yamlExtensionSettings = YamlExtensionSettings.parse(in);
      assertThat((String) yamlExtensionSettings.getSetting("foo", null)).isEqualTo("fooValue");
      assertThat(yamlExtensionSettings.getSetting("foo", "default")).isEqualTo("fooValue");
      assertThat((String) yamlExtensionSettings.getSetting("bar", null)).isEqualTo("barValue");
      assertThat(yamlExtensionSettings.getSetting("bar", "default")).isEqualTo("barValue");
      assertThat(yamlExtensionSettings.getSetting("baz", "bazValue")).isEqualTo("bazValue");
  }
}
