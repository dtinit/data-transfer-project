package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityLocation;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.junit.Test;

public class SocialPostsSerializerTest extends GenericImportSerializerTestBase {
  @Test
  public void testSocialPostsSerializer() throws Exception {
    SocialActivityContainerResource container =
        new SocialActivityContainerResource(
            "123",
            new SocialActivityActor("321", "Steve", null),
            Arrays.asList(
                new SocialActivityModel(
                    "456",
                    Instant.ofEpochSecond(1732713392),
                    SocialActivityType.NOTE,
                    Arrays.asList(
                        new SocialActivityAttachment(
                            SocialActivityAttachmentType.IMAGE, "foo.com", "Foo", null)),
                    new SocialActivityLocation("foo", 10, 10),
                    "Hello world!",
                    "Hi there",
                    null)));

    List<ImportableData<SocialPostsSerializer.ExportData>> res =
        iterableToList(SocialPostsSerializer.serialize(container));

    assertEquals(1, res.size());
    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"SocialActivity\","
            + "  \"metadata\": {"
            + "    \"@type\": \"SocialActivityMetadata\","
            + "    \"actor\": {"
            + "      \"@type\": \"SocialActivityActor\","
            + "      \"id\": \"321\","
            + "      \"name\": \"Steve\","
            + "      \"url\": null"
            + "    }"
            + "  },"
            + "  \"activity\": {"
            + "    \"@type\": \"SocialActivityModel\","
            + "    \"id\": \"456\","
            + "    \"published\": \"2024-11-27T13:16:32Z\","
            + "    \"type\": \"NOTE\","
            + "    \"attachments\": ["
            + "      {"
            + "        \"@type\": \"SocialActivityAttachment\","
            + "        \"type\": \"IMAGE\","
            + "        \"url\": \"foo.com\","
            + "        \"name\": \"Foo\","
            + "        \"content\": null"
            + "      }"
            + "    ],"
            + "    \"location\": {"
            + "      \"@type\": \"SocialActivityLocation\","
            + "      \"name\": \"foo\","
            + "      \"longitude\": 10.0,"
            + "      \"latitude\": 10.0"
            + "    },"
            + "    \"title\": \"Hello world!\","
            + "    \"content\": \"Hi there\","
            + "    \"url\": null"
            + "  }"
            + "}",
        res.get(0).getJsonData());
  }
}
