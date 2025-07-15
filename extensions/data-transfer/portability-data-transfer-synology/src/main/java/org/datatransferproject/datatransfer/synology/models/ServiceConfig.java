/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import java.util.Objects;

/** Data model for synology service configuration. */
public class ServiceConfig {
  private final Retry retry;
  private final Map<String, Service> services;

  @JsonCreator
  public ServiceConfig(
      @JsonProperty("retry") Retry retry, @JsonProperty("services") Map<String, Service> services) {
    this.retry = Objects.requireNonNull(retry, "retry is required");
    this.services = Objects.requireNonNull(services, "at least 1 service is required");
  }

  public Retry getRetry() {
    return retry;
  }

  public Map<String, Service> getServices() {
    return services;
  }

  public <T extends Service> T getServiceAs(String name, Class<T> type) {
    Service service = services.get(name);
    if (type.isInstance(service)) {
      return type.cast(service);
    } else {
      throw new IllegalArgumentException(
          "Service '" + name + "' is not of type " + type.getSimpleName());
    }
  }

  public static class Retry {
    private final int maxAttempts;

    @JsonCreator
    public Retry(@JsonProperty("maxAttempts") int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = C2Api.class, name = "c2"),
  })
  public static class Service {
    private final String baseUrl;

    @JsonCreator
    public Service(@JsonProperty("baseUrl") String baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl is required");
    }

    public String getBaseUrl() {
      return baseUrl;
    }
  }
}
