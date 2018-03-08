package org.dataportabilityproject.transfer.microsoft.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.dataportabilityproject.spi.transfer.types.TempCalendarData;
import org.junit.Assert;
import org.junit.Test;

/** */
public class TempCalendarDataTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    TempCalendarData calendarData =
        new TempCalendarData("job1", Collections.singletonMap("old1", "new1"));
    String serialized = objectMapper.writeValueAsString(calendarData);

    TempCalendarData deserialized = objectMapper.readValue(serialized, TempCalendarData.class);

    Assert.assertEquals("job1", deserialized.getJobId());
    Assert.assertEquals("new1", deserialized.getImportedId("old1"));
  }
}
