package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DataTransferResponseTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    DataTransferResponse transfer = new DataTransferResponse("testSource", "testDestination", "application/json",
        DataTransferResponse.Status.INPROCESS, "nexturltofollow");
    String serialized = objectMapper.writeValueAsString(transfer);

    DataTransferResponse deserialized = objectMapper.readValue(serialized, DataTransferResponse.class);

    Assert.assertEquals(DataTransferResponse.Status.INPROCESS, deserialized.getStatus());
    Assert.assertEquals("nexturltofollow", deserialized.getNextUrl());
  }
}