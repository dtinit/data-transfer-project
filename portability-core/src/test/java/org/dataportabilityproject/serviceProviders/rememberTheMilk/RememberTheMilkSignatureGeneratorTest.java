/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import static com.google.common.truth.Truth.assertThat;

import java.net.URL;
import org.dataportabilityproject.shared.AppCredentials;
import org.junit.Test;

public class RememberTheMilkSignatureGeneratorTest {
  private static final String KEY = "BANANAS1";
  private static final String SECRET = "BANANAS2";
  private static final String TOKEN = "BANANAS3";

  private static final AppCredentials APP_CREDENTIALS = AppCredentials.create(KEY, SECRET);
  private static final RememberTheMilkSignatureGenerator SIGNATURE_GENERATOR =
      new RememberTheMilkSignatureGenerator(APP_CREDENTIALS, TOKEN);

  @Test
  public void signatureTest() throws Exception {
    URL url = new URL("http://example.com?yxz=foo&feg=bar&abc=baz");
    URL expected =
        new URL(
            "http://example.com?yxz=foo&feg=bar&abc=baz&api_key=BANANAS1&auth_token=BANANAS3&api_sig=b48f0dd1a18179b3068b16728e214561");
    assertThat(SIGNATURE_GENERATOR.getSignature(url)).isEqualTo(expected);
  }
}
