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

package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.truth.Truth;
import org.junit.Test;

public class DateRangeContainerResourceTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(DateRangeContainerResource.class);

    int startDate = 1607784466;
    int endDate = 1608648466;

    ContainerResource data = new DateRangeContainerResource(startDate, endDate);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
            objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(DateRangeContainerResource.class);
    DateRangeContainerResource deserialized = (DateRangeContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getStartDate()).isEqualTo(startDate);
    Truth.assertThat(deserialized.getEndDate()).isEqualTo(endDate);
    Truth.assertThat(deserialized).isEqualTo(data);
  }
}
