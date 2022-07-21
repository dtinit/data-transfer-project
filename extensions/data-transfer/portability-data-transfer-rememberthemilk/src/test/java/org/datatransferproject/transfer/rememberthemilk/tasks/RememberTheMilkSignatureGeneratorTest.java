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

package org.datatransferproject.transfer.rememberthemilk.tasks;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.net.URL;
import java.util.Map;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.junit.jupiter.api.Test;

public class RememberTheMilkSignatureGeneratorTest {
  private static final String KEY = "BANANAS1";
  private static final String SECRET = "BANANAS2";
  private static final String TOKEN = "BANANAS3";

  private static final AppCredentials APP_CREDENTIALS = new AppCredentials(KEY, SECRET);
  private static final RememberTheMilkSignatureGenerator SIGNATURE_GENERATOR =
      new RememberTheMilkSignatureGenerator(APP_CREDENTIALS, TOKEN);

  @Test
  public void signatureTest() throws Exception {
    String base = "http://example.com";
    Map<String, String> queryParams = ImmutableMap.of("yxz", "foo", "feg", "bar", "abc", "baz");

    URL expected =
        new URL(
            base
                + "?abc=baz&api_key=BANANAS1&auth_token=BANANAS3&feg=bar&yxz=foo&api_sig=b48f0dd1a18179b3068b16728e214561");
    assertThat(SIGNATURE_GENERATOR.getSignature(base, queryParams)).isEqualTo(expected);
  }

  @Test
  public void signatureTestWhiteSpace() throws Exception{
    String base = "http://example.com";
    Map<String, String> queryParams = ImmutableMap.of("abc", "baz", "foo", "b a r ");
    URL expected = new URL(base + "?abc=baz&api_key=BANANAS1&auth_token=BANANAS3&foo=b a r&api_sig=204b1dc1abbff7a5e8753d7102d966f4");
    assertThat(SIGNATURE_GENERATOR.getSignature(base, queryParams)).isEqualTo(expected);
  }


}
