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

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;

/**
 * Maps from a Graph API dateTimeTimeZone resource as defined by microsoft.graph.dateTimeTimeZone
 * defined at
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/dateTimeTimeZone
 */
public class ToCalendarEventTimeTransformer
    implements BiFunction<
        Map<String, String>, TransformerContext, CalendarEventModel.CalendarEventTime> {

  @Override
  public CalendarEventModel.CalendarEventTime apply(
      Map<String, String> time, TransformerContext context) {
    if (time == null) {
      return null;
    }

    String dateTimeValue = time.get("dateTime");
    String timeZone = time.get("timeZone");
    if (dateTimeValue == null || timeZone == null) {
      return null;
    }

    try {
      OffsetDateTime dateTime =
          ZonedDateTime.of(LocalDateTime.parse(dateTimeValue), ZoneId.of(timeZone))
              .toOffsetDateTime();
      return new CalendarEventModel.CalendarEventTime(dateTime, false);
    } catch (DateTimeException e) {
      context.problem(e.getMessage());
      return null;
    }
  }
}
