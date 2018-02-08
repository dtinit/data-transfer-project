/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import org.dataportabilityproject.transfer.microsoft.helper.TestTransformerContext;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ToCalendarAttendeeModelTransformerTest {

    private ToCalendarAttendeeModelTransformer transformer = new ToCalendarAttendeeModelTransformer();

    @Test
    public void testTransform() {
        Map<String, Object> attendeeMap = new HashMap<>();
        attendeeMap.put("type", "required");
        Map<String, String> addressMap = new HashMap<>();
        addressMap.put("name", "Test Test1");
        addressMap.put("address", "foo@foo.com");
        attendeeMap.put("emailAddress", addressMap);

        CalendarAttendeeModel attendee = transformer.apply(attendeeMap, new TestTransformerContext());

        Assert.assertFalse(attendee.getOptional());
        Assert.assertEquals("Test Test1", attendee.getDisplayName());
        Assert.assertEquals("foo@foo.com", attendee.getEmail());
    }

}