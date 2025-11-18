package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel.CalendarEventTime;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.junit.Test;

public class CalendarSerializerTest extends GenericImportSerializerTestBase {
  @Test
  public void testCalendarSerializer() throws Exception {
    CalendarContainerResource container =
        new CalendarContainerResource(
            List.of(new CalendarModel("calendar123", "Calendar 123", "Calendar description")),
            List.of(
                new CalendarEventModel(
                    "calendar123",
                    "Event 1",
                    "Event notes",
                    Arrays.asList(
                        new CalendarAttendeeModel("attendee1", "attendee1@example.com", false),
                        new CalendarAttendeeModel("attendee2", "attendee2@example.com", true)),
                    "Event Place",
                    new CalendarEventTime(
                        OffsetDateTime.ofInstant(Instant.ofEpochSecond(1732713392), ZoneOffset.UTC),
                        false),
                    new CalendarEventTime(
                        OffsetDateTime.ofInstant(
                            Instant.ofEpochSecond(1732713392 + 60 * 60 * 2), ZoneOffset.UTC),
                        false),
                    null)));

    List<ImportableData<CalendarSerializer.ExportData>> res =
        iterableToList(CalendarSerializer.serialize(container));

    assertEquals(2, res.size());

    assertJsonEquals(
        "{"
            + "  \"@type\": \"Calendar\","
            + "  \"id\": \"calendar123\","
            + "  \"name\": \"Calendar 123\","
            + "  \"description\": \"Calendar description\""
            + "}",
        res.get(0).getJsonData());

    assertJsonEquals(
        "{"
            + "  \"@type\": \"CalendarEvent\","
            + "  \"calendarId\": \"calendar123\","
            + "  \"title\": \"Event 1\","
            + "  \"notes\": \"Event notes\","
            + "  \"attendees\": ["
            + "    {"
            + "      \"@type\": \"CalendarAttendeeModel\","
            + "      \"displayName\": \"attendee1\","
            + "      \"email\": \"attendee1@example.com\","
            + "      \"optional\": false"
            + "    },"
            + "    {"
            + "      \"@type\": \"CalendarAttendeeModel\","
            + "      \"displayName\": \"attendee2\","
            + "      \"email\": \"attendee2@example.com\","
            + "      \"optional\": true"
            + "    }"
            + "  ],"
            + "  \"location\": \"Event Place\","
            + "  \"startTime\": {"
            + "    \"@type\": \"CalendarEventModel$CalendarEventTime\","
            + "    \"dateTime\": \"2024-11-27T13:16:32Z\","
            + "    \"dateOnly\": false"
            + "  },"
            + "  \"endTime\": {"
            + "    \"@type\": \"CalendarEventModel$CalendarEventTime\","
            + "    \"dateTime\": \"2024-11-27T15:16:32Z\","
            + "    \"dateOnly\": false"
            + "  },"
            + "  \"recurrenceRule\": null"
            + "}",
        res.get(1).getJsonData());
  }
}
