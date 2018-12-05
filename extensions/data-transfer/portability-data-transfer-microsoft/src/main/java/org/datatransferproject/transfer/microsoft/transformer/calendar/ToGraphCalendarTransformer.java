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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarModel;

/**
 * Maps from a transfer calendar type to a Graph API calendar resource as defined by:
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/calendar.
 */
public class ToGraphCalendarTransformer
    implements BiFunction<CalendarModel, TransformerContext, Map<String, String>> {

  @Override
  public Map<String, String> apply(CalendarModel calendarModel, TransformerContext context) {
    Map<String, String> calendar = new LinkedHashMap<>();
    calendar.put("name", calendarModel.getName());
    return calendar;
  }
}
