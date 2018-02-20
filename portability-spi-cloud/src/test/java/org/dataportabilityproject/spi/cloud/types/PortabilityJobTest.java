package org.dataportabilityproject.spi.cloud.types;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob.State;
import org.dataportabilityproject.test.types.ObjectMapperFactory;
import org.junit.Test;

/**
 * Tests serialization and deserialization of a {@link PortabilityJob}.
 */
public class PortabilityJobTest {

    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
        LocalDateTime date = LocalDateTime.of(2018, 2, 20, 12, 0);

        JobAuthorization jobAuthorization = JobAuthorization.builder()
            .setState(JobAuthorization.State.INITIAL).setEncryptedSessionKey("foo").build();

        PortabilityJob job = PortabilityJob.builder()
            .setState(State.NEW)
            .setExportService("fooService")
            .setImportService("barService")
            .setTransferDataType("photos")
            .setCreatedTimestamp(date)
            .setLastUpdateTimestamp(date.plusMinutes(2))
            .setJobAuthorization(jobAuthorization).build();

        String serializedJobAuthorization = objectMapper.writeValueAsString(jobAuthorization);
        JobAuthorization deserializedJobAuthorization =
            objectMapper.readValue(serializedJobAuthorization, JobAuthorization.class);
        assertThat(deserializedJobAuthorization).isEqualTo(jobAuthorization);

        String serializedJob = objectMapper.writeValueAsString(job);
        PortabilityJob deserializedJob =
            objectMapper.readValue(serializedJob, PortabilityJob.class);
        assertThat(deserializedJob).isEqualTo(job);
    }
}