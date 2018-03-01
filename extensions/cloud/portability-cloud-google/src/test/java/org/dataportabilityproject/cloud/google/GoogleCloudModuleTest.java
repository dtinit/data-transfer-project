/*
 * Copyright 2018 The Data-Portability Project Authors.
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

package org.dataportabilityproject.cloud.google;


import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dataportabilityproject.cloud.google.GoogleCloudModule.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoogleCloudModuleTest {

  @Test
  public void getProjectEnvironment() {
    assertThat(GoogleCloudModule.getProjectEnvironment("acme-qa")).isEqualTo(Environment.QA);
  }

  @Test
  public void getProjectEnvironment_missingEnvironment() {
    assertThrows(IllegalArgumentException.class,
        () -> GoogleCloudModule.getProjectEnvironment("acme"));
  }

  @Test
  public void getProjectEnvironment_invalidEnvironment() {
    assertThrows(IllegalArgumentException.class,
        () -> GoogleCloudModule.getProjectEnvironment("acme-notARealEnvironment"));
  }
}
