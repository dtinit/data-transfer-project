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

package org.datatransferproject.datatransfer.synology.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.datatransferproject.datatransfer.synology.models.C2Api;
import org.datatransferproject.datatransfer.synology.models.ServiceConfig;

public class TestConfigs {
  public static final String TEST_C2_BASE_URL = "https://fake.url";
  public static final String TEST_CREATE_ALBUM_PATH = "/create";
  public static final String TEST_UPLOAD_ITEM_PATH = "/upload";
  public static final String TEST_ADD_ITEM_TO_ALBUM_PATH = "/add";
  public static final int TEST_MAX_ATTEMPTS = 5;

  public static ServiceConfig createServiceConfig() {
    C2Api.ApiPath apiPath =
        new C2Api.ApiPath(
            TEST_CREATE_ALBUM_PATH, TEST_UPLOAD_ITEM_PATH, TEST_ADD_ITEM_TO_ALBUM_PATH);

    C2Api c2Api = new C2Api(TEST_C2_BASE_URL, apiPath);

    Map<String, ServiceConfig.Service> serviceMap = new HashMap<>();
    serviceMap.put("c2", c2Api);

    ServiceConfig.Retry retry = new ServiceConfig.Retry(TEST_MAX_ATTEMPTS);

    return new ServiceConfig(retry, serviceMap);
  }

  public static Optional<JsonNode> createServiceConfigJson() {
    ObjectMapper mapper = new ObjectMapper();
    return Optional.of(mapper.valueToTree(createServiceConfig()));
  }
}
