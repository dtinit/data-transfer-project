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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.datatransferproject.datatransfer.synology.models.ServiceConfig;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

public class ServiceConfigParser {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static ServiceConfig parse(TransferServiceConfig config) {
    return parse(config.getServiceConfig());
  }

  public static ServiceConfig parse(JsonNode node) {
    try {
      return mapper.treeToValue(node, ServiceConfig.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse serviceConfig into ServiceConfig object", e);
    }
  }

  public static ServiceConfig parse(Optional<JsonNode> maybeNode) {
    return maybeNode
        .map(ServiceConfigParser::parse)
        .orElseThrow(() -> new IllegalArgumentException("ServiceConfig is required"));
  }
}
