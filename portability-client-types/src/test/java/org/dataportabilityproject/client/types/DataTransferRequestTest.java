package org.dataportabilityproject.client.types;

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

        String serialized = objectMapper.writeValueAsString(new DataTransferRequest("application/json"));

        DataTransferRequest deserialized = objectMapper.readValue(serialized, (DataTransferRequest.class));

        Assert.assertEquals("application/json", deserialized.getContentType());
    }
}