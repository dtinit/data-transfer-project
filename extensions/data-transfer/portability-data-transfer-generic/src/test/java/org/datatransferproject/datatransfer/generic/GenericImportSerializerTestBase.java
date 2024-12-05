package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.BeforeClass;

class GenericImportSerializerTestBase {
  static ObjectMapper objectMapper = new ObjectMapper();

  @BeforeClass
  public static void onlyOnce() {
    GenericImporter.configureObjectMapper(objectMapper);
  }

  <T> void assertJsonEquals(String expectedPayload, GenericPayload<T> actualWrapperPayload)
      throws Exception {
    assertNotNull(actualWrapperPayload.getApiVersion());
    assertNotNull(actualWrapperPayload.getSchemaSource());
    assertEquals(
        objectMapper.readTree(expectedPayload),
        // Wrap/unwrap to compare just what gets serialized, which is the most important thing
        objectMapper.readTree(objectMapper.writeValueAsString(actualWrapperPayload.getPayload())));
  }

  static <T> List<T> iterableToList(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }
}
