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

import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarModel;

/**
 * Maps from a Graph API calendar resource as defined by:
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/resources/calendar.
 */
public class ToCalendarModelTransformer
    implements BiFunction<Map<String, Object>, TransformerContext, CalendarModel> {

  @Override
  public CalendarModel apply(Map<String, Object> calendar, TransformerContext context) {
    String id = (String) calendar.get("id");
    if (id == null) {
      context.problem("Calendar id not found");
      return null;
    }

    String name = (String) calendar.get("name");
    if (name == null) {
      context.problem("Calendar name not found");
      return null;
    }
    return new CalendarModel(
        id, name, name); // use name as description since it does not exist in Office
  }
}
