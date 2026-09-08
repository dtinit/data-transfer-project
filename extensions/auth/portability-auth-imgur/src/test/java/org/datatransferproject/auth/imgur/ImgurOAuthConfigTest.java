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

package org.datatransferproject.auth.imgur;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;
import org.junit.jupiter.api.Test;

public class ImgurOAuthConfigTest {

  private static Optional<JsonNode> serviceConfig(String yaml) throws IOException {
    return TransferServiceConfig.create(new ByteArrayInputStream(yaml.getBytes(UTF_8)))
        .getServiceConfig();
  }

  @Test
  public void defaultsToImgursOwnEndpoints() {
    ImgurOAuthConfig config = new ImgurOAuthConfig(Optional.empty());

    assertThat(config.getAuthUrl()).isEqualTo(ImgurOAuthConfig.DEFAULT_AUTH_URL);
    assertThat(config.getTokenUrl()).isEqualTo(ImgurOAuthConfig.DEFAULT_TOKEN_URL);
  }

  @Test
  public void readsBothEndpointsFromServiceConfig() throws IOException {
    ImgurOAuthConfig config =
        new ImgurOAuthConfig(
            serviceConfig(
                "serviceConfig:\n"
                    + "  authUrl: \"https://imgur.example/oauth2/authorize\"\n"
                    + "  tokenUrl: \"https://imgur.example/oauth2/token\"\n"));

    assertThat(config.getAuthUrl()).isEqualTo("https://imgur.example/oauth2/authorize");
    assertThat(config.getTokenUrl()).isEqualTo("https://imgur.example/oauth2/token");
  }

  @Test
  public void overridesEachEndpointIndependently() throws IOException {
    // Only tokenUrl is strictly load-bearing -- generateAuthData dereferences it,
    // while the auth URL is only ever handed to a browser -- so overriding one
    // without the other has to leave the other at its default rather than empty.
    ImgurOAuthConfig config =
        serviceConfigured("serviceConfig:\n  tokenUrl: \"https://imgur.example/oauth2/token\"\n");

    assertThat(config.getTokenUrl()).isEqualTo("https://imgur.example/oauth2/token");
    assertThat(config.getAuthUrl()).isEqualTo(ImgurOAuthConfig.DEFAULT_AUTH_URL);
  }

  @Test
  public void keepsDefaultsWhenTheConfigHasNoServiceSection() throws IOException {
    ImgurOAuthConfig config = serviceConfigured("perUserRateLimit: 10");

    assertThat(config.getAuthUrl()).isEqualTo(ImgurOAuthConfig.DEFAULT_AUTH_URL);
    assertThat(config.getTokenUrl()).isEqualTo(ImgurOAuthConfig.DEFAULT_TOKEN_URL);
  }

  @Test
  public void keepsTheServiceNameTheRegistryKeysOn() {
    // PortabilityAuthServiceProviderRegistry does an exact-match lookup on this
    // string, so a change here silently breaks every Imgur job at creation time.
    assertThat(new ImgurOAuthConfig(Optional.empty()).getServiceName()).isEqualTo("Imgur");
  }

  private static ImgurOAuthConfig serviceConfigured(String yaml) throws IOException {
    return new ImgurOAuthConfig(serviceConfig(yaml));
  }
}
