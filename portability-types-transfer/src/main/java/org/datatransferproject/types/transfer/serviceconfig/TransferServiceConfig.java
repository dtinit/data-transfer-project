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

package org.datatransferproject.types.transfer.serviceconfig;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A wrapper around {@link TransferServiceConfigSpecification} to provide service specific settings
 * for transfer extensions.
 */
public final class TransferServiceConfig {
  private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  private final RateLimiter rateLimiter;
  private final Optional<JsonNode> serviceConfig;

  public static TransferServiceConfig create(InputStream s) throws IOException {
    return new TransferServiceConfig(
        YAML_OBJECT_MAPPER.readValue(s, TransferServiceConfigSpecification.class));
  }

  public static TransferServiceConfig getForService(String service) throws IOException {
    InputStream stream =
        TransferServiceConfig.class
            .getClassLoader()
            .getResourceAsStream(format("config/%s.yaml", service.toLowerCase()));
    if (stream == null) {
      return TransferServiceConfig.getDefaultInstance();
    } else {
      return TransferServiceConfig.create(stream);
    }
  }

  /** Gets a default instance for services that don't have a specific config. * */
  public static TransferServiceConfig getDefaultInstance() {
    return new TransferServiceConfig(
        new TransferServiceConfigSpecification(Double.MAX_VALUE, null));
  }

  private TransferServiceConfig(TransferServiceConfigSpecification specification) {
    checkNotNull(specification, "specification can't be null");
    rateLimiter = RateLimiter.create(specification.getPerUserRateLimit());
    serviceConfig = specification.getServiceConfig();
  }

  /**
   * A {@link RateLimiter} that enforces the per-user rate limit that is specified in the
   * config/[service].yaml config file.
   */
  public RateLimiter getPerUserRateLimiter() {
    return rateLimiter;
  }

  /** Service-specific configuration * */
  public Optional<JsonNode> getServiceConfig() {
    return serviceConfig;
  }
}
