package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import org.dataportabilityproject.transfer.microsoft.helper.TestTransformerContext;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
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