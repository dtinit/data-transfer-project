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
import org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;

/**
 * Maps from a Graph API calendar attendee resource as defined by
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/resources/attendee
 */
public class ToCalendarAttendeeModelTransformer
    implements BiFunction<Map<String, Object>, TransformerContext, CalendarAttendeeModel> {

  @Override
  @SuppressWarnings("unchecked")
  public CalendarAttendeeModel apply(Map<String, Object> attendee, TransformerContext context) {
    if (attendee == null) {
      return null;
    }

    boolean optional = !attendee.getOrDefault("type", "false").equals("required");
    Map<String, ?> emailAddress = (Map<String, ?>) attendee.get("emailAddress");
    String displayName = TransformerHelper.getString("name", emailAddress).orElse("");
    String email = TransformerHelper.getString("address", emailAddress).orElse("");

    return new CalendarAttendeeModel(displayName, email, optional);
  }
}
