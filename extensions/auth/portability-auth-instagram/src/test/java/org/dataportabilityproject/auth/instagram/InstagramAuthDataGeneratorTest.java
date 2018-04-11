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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

import com.google.api.client.http.HttpTransport;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Initial test for GoogleAuthDataGenerator. */
@RunWith(MockitoJUnitRunner.class)
public class InstagramAuthDataGeneratorTest {
  AppCredentials appCredentials = new AppCredentials("dummy-id", "dummy-secret");
  @Mock private HttpTransport httpTransport;

  @Test
  public void generateConfiguration() {
    InstagramAuthDataGenerator generator =
        new InstagramAuthDataGenerator(appCredentials, httpTransport);

    AuthFlowConfiguration config =
        generator.generateConfiguration("http://localhost/test", "54321");
    assertThat(config.getUrl())
        .isEqualTo(
            "https://api.instagram.com/oauth/authorize"
                + "?client_id=dummy-id"
                + "&redirect_uri=http://localhost/test/callback/instagram"
                + "&response_type=code"
                + "&scope=basic"
                + "&state=NTQzMjE%3D");
  }
}
