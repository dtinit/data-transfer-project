package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DataTransferTest {

    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String serialized = objectMapper.writeValueAsString(new DataTransfer(DataTransfer.Status.INPROCESS));

        DataTransfer deserialized = objectMapper.readValue(serialized, DataTransfer.class);

        Assert.assertEquals(DataTransfer.Status.INPROCESS, deserialized.getStatus());
    }
}