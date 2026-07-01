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

package org.datatransferproject.transfer.amazon.photos.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelDeserializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  void amazonPhotosNode_deserializesIdAndName() throws Exception {
    String json = "{\"id\":\"nodeId1\",\"name\":\"photo.jpg\",\"kind\":\"FILE\","
        + "\"description\":\"A photo\"}";

    AmazonPhotosNode node = objectMapper.readValue(json, AmazonPhotosNode.class);

    assertEquals("nodeId1", node.getId());
    assertEquals("photo.jpg", node.getName());
  }

  @Test
  void amazonPhotosNode_ignoresUnknownFields() throws Exception {
    String json = "{\"id\":\"n1\",\"name\":\"test\",\"kind\":\"FOLDER\","
        + "\"unknownField\":\"value\",\"anotherUnknown\":123}";

    AmazonPhotosNode node = objectMapper.readValue(json, AmazonPhotosNode.class);

    assertEquals("n1", node.getId());
    assertEquals("test", node.getName());
  }

  @Test
  void endpointResponse_deserializesAllFields() throws Exception {
    String json = "{\"metadataUrl\":\"https://meta.example.com/v1/\","
        + "\"contentUrl\":\"https://content.example.com/\","
        + "\"uploadServiceUrl\":\"https://upload.example.com/\"}";

    EndpointResponse response = objectMapper.readValue(json, EndpointResponse.class);

    assertEquals("https://meta.example.com/v1/", response.getMetadataUrl());
    assertEquals("https://content.example.com/", response.getContentUrl());
    assertEquals("https://upload.example.com/", response.getUploadServiceUrl());
  }

  @Test
  void endpointResponse_handlesNullUploadServiceUrl() throws Exception {
    String json = "{\"metadataUrl\":\"https://meta.example.com/\","
        + "\"contentUrl\":\"https://content.example.com/\"}";

    EndpointResponse response = objectMapper.readValue(json, EndpointResponse.class);

    assertNull(response.getUploadServiceUrl());
  }

  @Test
  void createNodeRequest_serializesCorrectly() throws Exception {
    CreateNodeRequest request = new CreateNodeRequest("My Album", "VISUAL_COLLECTION");

    String json = objectMapper.writeValueAsString(request);

    assertTrue(json.contains("\"name\":\"My Album\""));
    assertTrue(json.contains("\"kind\":\"VISUAL_COLLECTION\""));
  }
}
