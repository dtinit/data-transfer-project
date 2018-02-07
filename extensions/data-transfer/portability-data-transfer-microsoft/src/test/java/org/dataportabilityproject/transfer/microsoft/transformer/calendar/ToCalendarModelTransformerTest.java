package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.transfer.microsoft.helper.TestTransformerContext;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerContext;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class ToCalendarModelTransformerTest {
    private ToCalendarModelTransformer transformer;
    private ObjectMapper mapper;
    private TransformerContext context;

    @Test
    @SuppressWarnings("unchecked")
    public void testTransform() throws IOException {
        Map<String, Object> rawEvent = mapper.readValue(JSON, Map.class);

        CalendarModel calendar = transformer.apply(rawEvent, context);
        
        Assert.assertEquals("123", calendar.getId());
        Assert.assertEquals("Calendar", calendar.getName());
        Assert.assertEquals("Calendar", calendar.getDescription());
    }

    @Before
    public void setUp() {
        transformer = new ToCalendarModelTransformer();
        mapper = new ObjectMapper();
        context = new TestTransformerContext();
    }

    private static final String JSON = "{\n" +
            "            \"id\": \"123\",\n" +
            "            \"name\": \"Calendar\",\n" +
            "            \"color\": \"auto\",\n" +
            "            \"changeKey\": \"1\",\n" +
            "            \"canShare\": true,\n" +
            "            \"canViewPrivateItems\": true,\n" +
            "            \"canEdit\": true,\n" +
            "            \"owner\": {\n" +
            "                \"name\": \"Foo Bar\",\n" +
            "                \"address\": \"foo@outlook.com\"\n" +
            "            }\n" +
            "        }";
}