package org.dataportabilityproject.types.client.transfer;

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

    DataTransfer transfer = new DataTransfer("testSource", "testDestination", "application/json",
        DataTransfer.Status.INPROCESS, "nexturltofollow");
    String serialized = objectMapper.writeValueAsString(transfer);

    DataTransfer deserialized = objectMapper.readValue(serialized, DataTransfer.class);

    Assert.assertEquals(DataTransfer.Status.INPROCESS, deserialized.getStatus());
    Assert.assertEquals("nexturltofollow", deserialized.getNextURL());
  }
}