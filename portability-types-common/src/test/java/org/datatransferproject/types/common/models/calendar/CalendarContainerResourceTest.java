package org.datatransferproject.types.common.models.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel.CalendarEventTime;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class CalendarContainerResourceTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerSubtypes(CalendarContainerResource.class);

    CalendarEventTime today =
        new CalendarEventTime(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), true);

    List<CalendarModel> calendars =
        ImmutableList.of(new CalendarModel("id1", "name", "description"));

    List<CalendarEventModel> events =
        ImmutableList.of(
            new CalendarEventModel("id1", "event1", "A note", null, "Place1", today, today, null),
            new CalendarEventModel(
                "id1",
                "event2",
                null,
                ImmutableList.of(new CalendarAttendeeModel("Person", "a@gmail.com", false)),
                "place 2",
                today,
                today,
                null));

    ContainerResource data = new CalendarContainerResource(calendars, events);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
        objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(CalendarContainerResource.class);
    CalendarContainerResource deserialized = (CalendarContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getCalendars()).hasSize(1);
    Truth.assertThat(deserialized.getEvents()).hasSize(2);
    Truth.assertThat(deserialized).isEqualTo(data);
  }
}
