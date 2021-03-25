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

package org.datatransferproject.spi.cloud.types;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.Instant;
import java.util.TimeZone;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;
import org.datatransferproject.test.types.ObjectMapperFactory;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.Test;

/** Tests serialization and deserialization of a {@link PortabilityJob}. */
public class PortabilityJobTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    Instant date = Instant.now();

    JobAuthorization jobAuthorization =
        JobAuthorization.builder()
            .setState(JobAuthorization.State.INITIAL)
            .setSessionSecretKey("foo")
            .build();

    PortabilityJob job =
        PortabilityJob.builder()
            .setState(State.NEW)
            .setExportService("fooService")
            .setImportService("barService")
            .setTransferDataType("PHOTOS")
            .setCreatedTimestamp(date)
            .setLastUpdateTimestamp(date.plusSeconds(120))
            .setJobAuthorization(jobAuthorization)
            .build();

    String serializedJobAuthorization = objectMapper.writeValueAsString(jobAuthorization);
    JobAuthorization deserializedJobAuthorization =
        objectMapper.readValue(serializedJobAuthorization, JobAuthorization.class);
    assertThat(deserializedJobAuthorization).isEqualTo(jobAuthorization);

    String serializedJob = objectMapper.writeValueAsString(job);
    PortabilityJob deserializedJob = objectMapper.readValue(serializedJob, PortabilityJob.class);
    assertThat(deserializedJob).isEqualTo(job);
  }

  @Test
  public void verifySerializeDeserializeWithAlbum() throws IOException {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    Instant date = Instant.now();

    JobAuthorization jobAuthorization =
        JobAuthorization.builder()
            .setState(JobAuthorization.State.INITIAL)
            .setSessionSecretKey("foo")
            .build();

    PortabilityJob job =
        PortabilityJob.builder()
            .setState(State.NEW)
            .setExportService("fooService")
            .setImportService("barService")
            .setTransferDataType("PHOTOS")
            .setExportInformation(
                objectMapper.writeValueAsString(
                    new ExportInformation(
                        null,
                        new PhotosContainerResource(
                            Lists.newArrayList(
                                new PhotoAlbum("album_id", "album name", "album description")),
                            null))))
            .setCreatedTimestamp(date)
            .setLastUpdateTimestamp(date.plusSeconds(120))
            .setJobAuthorization(jobAuthorization)
            .build();

    String serializedJobAuthorization = objectMapper.writeValueAsString(jobAuthorization);
    JobAuthorization deserializedJobAuthorization =
        objectMapper.readValue(serializedJobAuthorization, JobAuthorization.class);
    assertThat(deserializedJobAuthorization).isEqualTo(jobAuthorization);

    String serializedJob = objectMapper.writeValueAsString(job);
    PortabilityJob deserializedJob = objectMapper.readValue(serializedJob, PortabilityJob.class);
    assertThat(deserializedJob).isEqualTo(job);
  }

  @Test
  public void verifySerializeDeserializeUserTimeZone() throws Exception {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    Instant date = Instant.now();

    JobAuthorization jobAuthorization =
        JobAuthorization.builder().setState(JobAuthorization.State.INITIAL).build();

    PortabilityJob job =
        PortabilityJob.builder()
            .setState(State.NEW)
            .setExportService("fooService")
            .setImportService("barService")
            .setTransferDataType("PHOTOS")
            .setCreatedTimestamp(date)
            .setLastUpdateTimestamp(date.plusSeconds(120))
            .setJobAuthorization(jobAuthorization)
            .setUserTimeZone(TimeZone.getTimeZone("America/Costa_Rica"))
            .build();

    String serializedJob = objectMapper.writeValueAsString(job);
    PortabilityJob deserializedJob = objectMapper.readValue(serializedJob, PortabilityJob.class);
    assertThat(deserializedJob).isEqualTo(job);
  }

  @Test
  public void verifySerializeDeserializeUserLocale() throws Exception {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    Instant date = Instant.now();

    JobAuthorization jobAuthorization =
        JobAuthorization.builder().setState(JobAuthorization.State.INITIAL).build();

    String userLocale = "it";

    PortabilityJob job =
        PortabilityJob.builder()
            .setState(State.NEW)
            .setExportService("fooService")
            .setImportService("barService")
            .setTransferDataType("PHOTOS")
            .setCreatedTimestamp(date)
            .setLastUpdateTimestamp(date.plusSeconds(120))
            .setJobAuthorization(jobAuthorization)
            .setUserLocale(userLocale)
            .build();

    String serializedJob = objectMapper.writeValueAsString(job);
    PortabilityJob deserializedJob = objectMapper.readValue(serializedJob, PortabilityJob.class);
    assertThat(deserializedJob.userLocale()).isEqualTo(userLocale);
    assertThat(deserializedJob).isEqualTo(job);
  }
}
