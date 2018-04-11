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
package org.dataportabilityproject.auth.instagram;

import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/** Initial test for GoogleAuthDataGenerator. */
@RunWith(MockitoJUnitRunner.class)
public class InstagramAuthDataGeneratorTest {

  @Test
  public void generateConfiguration() {
    assertNull(new InstagramAuthDataGenerator().generateConfiguration(null, null));
  }

  @Test
  public void generateAuthData() {
    assertNull(new InstagramAuthDataGenerator().generateAuthData(null, null, null, null, null));
  }



}
