package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DataTransferRequestTest {

    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String serialized = objectMapper.writeValueAsString(new DataTransferRequest("testSource", "testDestination", "application/json"));

        DataTransferRequest deserialized = objectMapper.readValue(serialized, DataTransferRequest.class);

        Assert.assertEquals("testSource", deserialized.getSource());
        Assert.assertEquals("testDestination", deserialized.getDestination());
        Assert.assertEquals("application/json", deserialized.getTransferDataType());
    }
}