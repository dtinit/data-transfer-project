package org.datatransferproject.types.common.models;

import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DataVerticalTest {

  @Test
  void shouldSerializeSocialPostsCorrectly() throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    String json = "\"SOCIAL-POSTS\"";
    assertEquals(SOCIAL_POSTS, om.readValue(json, DataVertical.class));
    assertEquals(SOCIAL_POSTS, DataVertical.fromDataType("SOCIAL-POSTS"));
    assertEquals(json, om.writeValueAsString(SOCIAL_POSTS));
  }
}
