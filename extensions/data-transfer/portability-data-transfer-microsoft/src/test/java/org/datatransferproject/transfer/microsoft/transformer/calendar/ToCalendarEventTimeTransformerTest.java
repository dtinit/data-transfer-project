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

import java.util.HashMap;
import java.util.Map;
import org.datatransferproject.transfer.microsoft.helper.TestTransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.junit.jupiter.api.Test;

public class ToCalendarEventTimeTransformerTest {
  private ToCalendarEventTimeTransformer transformer = new ToCalendarEventTimeTransformer();

  @Test
  public void testTransform() {
    Map<String, String> map = new HashMap<>();
    map.put("dateTime", "2018-02-14T18:00:00.0000000");
    map.put("timeZone", "UTC");
    CalendarEventModel.CalendarEventTime time =
        transformer.apply(map, new TestTransformerContext());

    assertEquals(18, time.getDateTime().getHour());
    assertEquals(2018, time.getDateTime().getYear());
    assertEquals(14, time.getDateTime().getDayOfMonth());
  }
}
