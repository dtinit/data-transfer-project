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

package org.datatransferproject.auth.facebook;

import com.google.api.client.http.HttpTransport;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol.OAUTH_2;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FacebookAuthDataGeneratorTest {
  @Mock private HttpTransport httpTransport;

  @Test
  public void testGenerateConfiguration() {
    FacebookAuthDataGenerator generator =
        new FacebookAuthDataGenerator(new AppCredentials("key", "secret"), httpTransport);

    AuthFlowConfiguration config =
        generator.generateConfiguration("http://localhost/test", "12345");

    assertEquals(
        "https://www.facebook.com/v3.1/dialog/oauth"
            + "?client_id=key"
            + "&redirect_uri=http://localhost/test/callback/facebook"
            + "&response_type=code"
            + "&scope=user_photos"
            + "&state=MTIzNDU%3D",
        config.getAuthUrl());
    assertEquals(OAUTH_2, config.getAuthProtocol());
  }
}
