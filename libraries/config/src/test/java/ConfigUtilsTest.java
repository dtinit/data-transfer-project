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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.dataportabilityproject.config.ConfigUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigUtilsTest {
  @Test
  public void parse() {
    String testYaml = "environment: PROD\ncloud: LOCAL";
    InputStream in = new ByteArrayInputStream(testYaml.getBytes(StandardCharsets.UTF_8));
    try {
      Map<String, Object> config = ConfigUtils.parseExtensionSettings(in);
      assertThat(config.get("environment")).isEqualTo("PROD");
      assertThat(config.get("cloud")).isEqualTo("LOCAL");
    } catch (IOException e) {
      fail("Could not parse test yaml. Got exception: " + e);
    }
  }
}
