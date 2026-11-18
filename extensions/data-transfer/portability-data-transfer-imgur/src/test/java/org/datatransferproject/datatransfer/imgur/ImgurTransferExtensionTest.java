/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.imgur;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;
import org.junit.jupiter.api.Test;

public class ImgurTransferExtensionTest {

  private static TransferServiceConfig configFrom(String yaml) throws IOException {
    return TransferServiceConfig.create(new ByteArrayInputStream(yaml.getBytes(UTF_8)));
  }

  @Test
  public void usesImgursOwnApiWhenNothingIsConfigured() {
    assertThat(ImgurTransferExtension.baseUrl(TransferServiceConfig.getDefaultInstance()))
        .isEqualTo(ImgurTransferExtension.DEFAULT_BASE_URL);
  }

  @Test
  public void usesImgursOwnApiWhenTheConfigHasNoServiceSection() throws IOException {
    // A config file that only sets a rate limit is the shape Flickr and Deezer
    // ship, so it must not be read as "override the base URL with nothing".
    assertThat(ImgurTransferExtension.baseUrl(configFrom("perUserRateLimit: 10")))
        .isEqualTo(ImgurTransferExtension.DEFAULT_BASE_URL);
  }

  @Test
  public void readsTheBaseUrlFromServiceConfig() throws IOException {
    TransferServiceConfig config =
        configFrom("serviceConfig:\n  baseUrl: \"https://imgur.example/3\"\n");

    assertThat(ImgurTransferExtension.baseUrl(config)).isEqualTo("https://imgur.example/3");
  }

  @Test
  public void ignoresAServiceConfigThatSetsOtherKeys() throws IOException {
    TransferServiceConfig config = configFrom("serviceConfig:\n  tokenUrl: \"https://x/token\"\n");

    assertThat(ImgurTransferExtension.baseUrl(config))
        .isEqualTo(ImgurTransferExtension.DEFAULT_BASE_URL);
  }
}
