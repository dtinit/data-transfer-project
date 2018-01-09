package org.dataportabilityproject.client.types.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ServiceProviderTest {
    @Test
    public void verifySerializeDeserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ServiceProvider provider = new ServiceProvider("123", "TestProvider", "Test description", new String[]{"testContent"});
        String serialized = objectMapper.writeValueAsString(provider);

        ServiceProvider deserialized = objectMapper.readValue(serialized, ServiceProvider.class);

        Assert.assertEquals("123", deserialized.getId());
        Assert.assertEquals("TestProvider", deserialized.getName());
        Assert.assertEquals("Test description", deserialized.getDescription());
        Assert.assertEquals("testContent", deserialized.getContentTypes()[0]);
    }

}