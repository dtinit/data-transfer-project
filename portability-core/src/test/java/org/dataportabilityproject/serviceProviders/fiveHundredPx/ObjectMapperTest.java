package org.dataportabilityproject.serviceProviders.fiveHundredPx;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import org.junit.Test;

public class ObjectMapper {

  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  public void testObjectMapper() throws IOException {

  }
}
