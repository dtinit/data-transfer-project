/*
* Copyright 2018 The Data-Portability Project Authors.
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

package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/* Test for DataTransferRequest */
public class DataTransferRequestTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    String serialized = objectMapper.writeValueAsString(
        new DataTransferRequest("testSource", "testDestination", "application/json"));

    DataTransferRequest deserialized = objectMapper
        .readValue(serialized, DataTransferRequest.class);

    Assert.assertEquals("testSource", deserialized.getSource());
    Assert.assertEquals("testDestination", deserialized.getDestination());
    Assert.assertEquals("application/json", deserialized.getTransferDataType());
  }
}
