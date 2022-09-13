package org.datatransferproject.spi.transfer.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.datatransferproject.types.common.IntPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.junit.jupiter.api.Test;

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

    assertNotNull(deserialized);
    assertEquals(100, ((IntPaginationToken) deserialized.getPaginationData()).getStart());
    assertEquals(
        "123", ((IdOnlyContainerResource) deserialized.getContainerResources().get(0)).getId());
  }
}
