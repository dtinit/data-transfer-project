package org.datatransferproject.datatransfer.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.BeforeClass;

class GenericImportSerializerTestBase {
  static ObjectMapper objectMapper = new ObjectMapper();

  @BeforeClass
  public static void onlyOnce() {
    objectMapper.registerModule(new JavaTimeModule());
    // TODO: this probably shouldn't live here
    objectMapper.addMixIn(VideoModel.class, MediaSkipFieldsMixin.class);
    objectMapper.addMixIn(PhotoModel.class, MediaSkipFieldsMixin.class);
  }

  void assertJsonEquals(String expectedPayload, JsonNode actualWrapperPayload) throws Exception {
    assertEquals("GenericPayload", actualWrapperPayload.get("@type").asText());
    assertFalse(actualWrapperPayload.get("apiVersion").isNull());
    assertFalse(actualWrapperPayload.get("schemaSource").isNull());
    assertEquals(
        objectMapper.readTree(expectedPayload),
        // Wrap/unwrap to compare just what gets serialized, which is the most important thing
        objectMapper.readTree(
            objectMapper.writeValueAsString(actualWrapperPayload.get("payload"))));
  }

  static <T> List<T> iterableToList(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }
}
