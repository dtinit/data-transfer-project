/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.transfer.microsoft.transformer.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.datatransferproject.transfer.microsoft.helper.TestTransformerContext;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ToCalendarModelTransformerTest {
  private static final String SAMPLE_CALENDAR =
      "{\n"
          + "            \"id\": \"123\",\n"
          + "            \"name\": \"Calendar\",\n"
          + "            \"color\": \"auto\",\n"
          + "            \"changeKey\": \"1\",\n"
          + "            \"canShare\": true,\n"
          + "            \"canViewPrivateItems\": true,\n"
          + "            \"canEdit\": true,\n"
          + "            \"owner\": {\n"
          + "                \"name\": \"Foo Bar\",\n"
          + "                \"address\": \"foo@outlook.com\"\n"
          + "            }\n"
          + "        }";
  private ToCalendarModelTransformer transformer;
  private ObjectMapper mapper;
  private TransformerContext context;

  @Test
  @SuppressWarnings("unchecked")
  public void testTransform() throws IOException {
    Map<String, Object> rawEvent = mapper.readValue(SAMPLE_CALENDAR, Map.class);

    CalendarModel calendar = transformer.apply(rawEvent, context);

    assertEquals("123", calendar.getId());
    assertEquals("Calendar", calendar.getName());
    assertEquals("Calendar", calendar.getDescription());
  }

  @BeforeEach
  public void setUp() {
    transformer = new ToCalendarModelTransformer();
    mapper = new ObjectMapper();
    context = new TestTransformerContext();
  }
}
