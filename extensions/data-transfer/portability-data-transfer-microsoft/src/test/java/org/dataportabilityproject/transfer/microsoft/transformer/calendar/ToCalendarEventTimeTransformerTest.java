package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 8601
 */
public class ToCalendarEventModelTransformerTest {

    @Test
    public void testTime() {
        String dateTime = "2018-02-14T18:00:00.0000000";
        String timeZone = "UTC";
        OffsetDateTime offsetDateTime = ZonedDateTime.of(LocalDateTime.parse(dateTime), ZoneId.of(timeZone)).toOffsetDateTime();


//        OffsetDateTime.of(LocalDateTime.parse("2018-02-14T18:00:00.0000000"), ZoneOffset.UTC);
//       ZonedDateTime zonedDateTime =  ZonedDateTime.of(LocalDateTime.parse(time), ZoneId.of(zone));
        System.out.println();
    }
}