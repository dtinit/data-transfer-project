/*
 * Copyright 2019 The Data Transfer Project Authors.
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

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

/**
 * Class that provides Imgur-specific information for OAuth2
 * See https://apidocs.imgur.com/#authorization-and-oauth
 *
 * <p>The authorization and token endpoints default to Imgur's, and may be overridden from
 * {@code config/imgur.yaml} on the classpath -- the same file {@code ImgurTransferExtension} reads
 * its {@code baseUrl} from. That exists so a deployer can point the adapter at a staging or test
 * double without rebuilding; nothing else changes behaviour.
 */
public class ImgurOAuthConfig implements OAuth2Config {

  private static final String SERVICE_NAME = "Imgur";

  @VisibleForTesting
  static final String DEFAULT_AUTH_URL = "https://api.imgur.com/oauth2/authorize";

  @VisibleForTesting
  static final String DEFAULT_TOKEN_URL = "https://api.imgur.com/oauth2/token";

  private final String authUrl;
  private final String tokenUrl;

  public ImgurOAuthConfig() {
    this(readServiceConfig());
  }

  @VisibleForTesting
  ImgurOAuthConfig(Optional<JsonNode> serviceConfig) {
    this.authUrl = configuredOrDefault(serviceConfig, "authUrl", DEFAULT_AUTH_URL);
    this.tokenUrl = configuredOrDefault(serviceConfig, "tokenUrl", DEFAULT_TOKEN_URL);
  }

  /**
   * Reads {@code config/imgur.yaml} if one is on the classpath.
   *
   * <p>Unlike the transfer extension, an {@link
   * org.datatransferproject.auth.OAuth2ServiceExtension} is handed no service-scoped {@code
   * TransferServiceConfig}, so this reads it directly. A missing or unreadable file is not an
   * error -- it just means the defaults apply.
   */
  private static Optional<JsonNode> readServiceConfig() {
    try {
      return TransferServiceConfig.getForService(SERVICE_NAME).getServiceConfig();
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static String configuredOrDefault(
      Optional<JsonNode> serviceConfig, String field, String fallback) {
    return serviceConfig.map(node -> node.path(field).asText(fallback)).orElse(fallback);
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  public String getAuthUrl() {
    return authUrl;
  }

  @Override
  public String getTokenUrl() {
    return tokenUrl;
  }

  // Imgur doesn't require scopes
  @Override
  public Map<DataVertical, Set<String>> getExportScopes() {
    return ImmutableMap.of(PHOTOS, ImmutableSet.of(""));
  }

  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.of(PHOTOS, ImmutableSet.of(""));
  }
}
