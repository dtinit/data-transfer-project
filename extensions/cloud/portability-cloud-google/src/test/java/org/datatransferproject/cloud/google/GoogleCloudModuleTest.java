/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.cloud.google;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.datatransferproject.api.launcher.Constants.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GoogleCloudModuleTest {

  @Test
  public void getProjectEnvironment() {
    assertThat(GoogleCloudExtensionModule.getProjectEnvironment("acme-qa"))
        .isEqualTo(Environment.QA);
  }

  @Test
  public void getProjectEnvironment_missingEnvironment() {
    assertThrows(
        IllegalArgumentException.class,
        () -> GoogleCloudExtensionModule.getProjectEnvironment("acme"));
  }

  @Test
  public void getProjectEnvironment_invalidEnvironment() {
    assertThrows(
        IllegalArgumentException.class,
        () -> GoogleCloudExtensionModule.getProjectEnvironment("acme-notARealEnvironment"));
  }
}
