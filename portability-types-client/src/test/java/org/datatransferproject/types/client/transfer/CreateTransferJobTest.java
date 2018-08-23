/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.types.client.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/* Test for CreateTransferJob */
public class CreateTransferJobTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    String serialized =
        objectMapper.writeValueAsString(
            new CreateTransferJob(
                    "testSource",
                    "testDestination",
                    "https://localhost:3000/callback/testSource",
                    "https://localhost:3000/callback/testDestination",
                    "PHOTOS"));

    CreateTransferJob deserialized =
        objectMapper.readValue(serialized, CreateTransferJob.class);

    Assert.assertEquals("testSource", deserialized.getExportService());
    Assert.assertEquals("testDestination", deserialized.getImportService());
    Assert.assertEquals("https://localhost:3000/callback/testSource", deserialized.getExportCallbackUrl());
    Assert.assertEquals("https://localhost:3000/callback/testDestination", deserialized.getImportCallbackUrl());
    Assert.assertEquals("PHOTOS", deserialized.getDataType());
  }
}
