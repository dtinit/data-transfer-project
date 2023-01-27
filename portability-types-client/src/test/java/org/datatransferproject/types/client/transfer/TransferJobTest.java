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
import org.datatransferproject.types.common.PortabilityCommon;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

/* Test for TransferJob */
public class TransferJobTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    TransferJob transfer =
        new TransferJob(
            "1-2-3",
            "testExportService",
            "testImportService",
            PHOTOS,
            "exportUrl",
            "importUrl",
            "exportTokenUrl",
            "importTokenUrl",
            PortabilityCommon.AuthProtocol.OAUTH_1,
            PortabilityCommon.AuthProtocol.OAUTH_2);
    String serialized = objectMapper.writeValueAsString(transfer);

    TransferJob deserialized =
        objectMapper.readValue(serialized, TransferJob.class);

    assertThat("1-2-3").isEqualTo(deserialized.getId());
    assertThat("testExportService").isEqualTo(deserialized.getExportService());
    assertThat("testImportService").isEqualTo(deserialized.getImportService());
    assertThat(PHOTOS).isEqualTo(deserialized.getDataType());
    assertThat("exportUrl").isEqualTo(deserialized.getExportUrl());
    assertThat("importUrl").isEqualTo(deserialized.getImportUrl());
    assertThat("exportTokenUrl").isEqualTo(deserialized.getExportTokenUrl());
    assertThat("importTokenUrl").isEqualTo(deserialized.getImportTokenUrl());
    assertThat(PortabilityCommon.AuthProtocol.OAUTH_1).isEqualTo(deserialized.getExportAuthProtocol());
    assertThat(PortabilityCommon.AuthProtocol.OAUTH_2).isEqualTo(deserialized.getImportAuthProtocol());
  }
}
