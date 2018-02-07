package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import org.dataportabilityproject.transfer.microsoft.helper.TestTransformerContext;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ToCalendarEventTimeTransformerTest {
    private ToCalendarEventTimeTransformer transformer = new ToCalendarEventTimeTransformer();

    @Test
    public void testTransform() {
        Map<String, String> map = new HashMap<>();
        map.put("dateTime", "2018-02-14T18:00:00.0000000");
        map.put("timeZone", "UTC");
        CalendarEventModel.CalendarEventTime time = transformer.apply(map, new TestTransformerContext());

        Assert.assertEquals(18, time.getDateTime().getHour());
        Assert.assertEquals(2018, time.getDateTime().getYear());
        Assert.assertEquals(14, time.getDateTime().getDayOfMonth());
    }
}