package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/** */
public class ContinuationDataTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(
        ContinuationData.class, IntPaginationToken.class, IdOnlyContainerResource.class);

    ContinuationData continuationData = new ContinuationData(new IntPaginationToken(100));
    continuationData.addContainerResource(new IdOnlyContainerResource("123"));

    String serialized = objectMapper.writeValueAsString(continuationData);

    ContinuationData deserialized = objectMapper.readValue(serialized, ContinuationData.class);

    Assert.assertNotNull(deserialized);
    Assert.assertEquals(100, ((IntPaginationToken) deserialized.getPaginationData()).getStart());
    Assert.assertEquals(
        "123", ((IdOnlyContainerResource) deserialized.getContainerResources().get(0)).getId());
  }
}
