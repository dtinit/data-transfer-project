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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.datatransferproject.datatransfer.synology.models.ServiceConfig.Service;
import org.junit.jupiter.api.Test;

public class ServiceConfigTest {
  @Test
  public void testServiceConfig() {
    ServiceConfig.Retry retryConfig = new ServiceConfig.Retry(3);
    Map<String, Service> services = Map.of("synologyService", new Service("http://example.com"));

    ServiceConfig config = new ServiceConfig(retryConfig, services);

    Service service = config.getServiceAs("synologyService", Service.class);
    assertNotNull(service);
    assertEquals("http://example.com", service.getBaseUrl());

    ServiceConfig.Retry retry = config.getRetry();
    assertNotNull(retry);
    assertEquals(3, retry.getMaxAttempts());

    Map<String, Service> serviceMap = config.getServices();
    assertNotNull(serviceMap);
    assertEquals(1, serviceMap.size());
    assertTrue(serviceMap.containsKey("synologyService"));
  }

  @Test
  public void testServiceConfigWithCustomService() {
    ServiceConfig.Retry retryConfig = new ServiceConfig.Retry(3);
    Map<String, Service> services = Map.of("customService", new CustomService("http://custom.com"));

    ServiceConfig config = new ServiceConfig(retryConfig, services);

    CustomService service = config.getServiceAs("customService", CustomService.class);
    assertNotNull(service);
    assertEquals("http://custom.com", service.getBaseUrl());

    ServiceConfig.Retry retry = config.getRetry();
    assertNotNull(retry);
    assertEquals(3, retry.getMaxAttempts());
  }

  public static class CustomService extends Service {
    public CustomService(String baseUrl) {
      super(baseUrl);
    }
  }
}
