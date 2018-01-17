package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.test.types.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

/**
 *
 */
public class PortabilityJobTest {

    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

        PortabilityJob job = new PortabilityJob();
        job.setId("123");
        LocalDateTime timestamp = LocalDateTime.now();
        job.setCreatedTimestamp(timestamp);

        String serialized = objectMapper.writeValueAsString(job);

        PortabilityJob deserialized = objectMapper.readValue(serialized, PortabilityJob.class);

        Assert.assertEquals(job.getId(), deserialized.getId());
        Assert.assertEquals(timestamp, deserialized.getCreatedTimestamp());
    }

}