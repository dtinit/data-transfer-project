package org.dataportabilityproject.types.transfer.models.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.time.OffsetDateTime;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel.CalendarEventTime;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.junit.Test;

public class CalendarModelWrapperTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerSubtypes(CalendarModelWrapper.class);

    CalendarEventTime today = new CalendarEventTime(OffsetDateTime.now(), true);

    List<CalendarModel> calendars = ImmutableList.of(
        new CalendarModel("id1", "name", "description")
    );

    List<CalendarEventModel> events = ImmutableList.of(
        new CalendarEventModel("id1", "event1", "A note",
            null, "Place1", today, today),
        new CalendarEventModel("id1", "event2", null,
            ImmutableList.of(new CalendarAttendeeModel("Person", "a@gmail.com", false)),
            "place 2", today, today)
    );

    DataModel data = new CalendarModelWrapper(calendars, events);

    String serialized = objectMapper.writeValueAsString(data);

    DataModel deserializedModel = objectMapper.readValue(serialized, DataModel.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(CalendarModelWrapper.class);
    CalendarModelWrapper deserialized = (CalendarModelWrapper) deserializedModel;
    Truth.assertThat(deserialized.getCalendars()).hasSize(1);
    Truth.assertThat(deserialized.getEvents()).hasSize(2);
  }
}
