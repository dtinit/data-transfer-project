package org.dataportabilityproject.types.client.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class RegisteredServiceProviderTest {
    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        RegisteredServiceProvider provider = new RegisteredServiceProvider("123", "TestProvider", "Test description", new String[]{"testContent"});
        String serialized = objectMapper.writeValueAsString(provider);

        RegisteredServiceProvider deserialized = objectMapper.readValue(serialized, RegisteredServiceProvider.class);

        Assert.assertEquals("123", deserialized.getId());
        Assert.assertEquals("TestProvider", deserialized.getName());
        Assert.assertEquals("Test description", deserialized.getDescription());
        Assert.assertEquals("testContent", deserialized.getTransferDataTypes()[0]);
    }

}