package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.Test;

public class MediaSerializerTest extends GenericImportSerializerTestBase {
  @Test
  public void testMediaSerializer() throws Exception {
    MediaContainerResource container =
        new MediaContainerResource(
            Arrays.asList(new MediaAlbum("album123", "Album 123", "Album description")),
            Arrays.asList(
                new PhotoModel(
                    "bar.jpeg",
                    "https://example.com/bar.jpg",
                    "Bar description",
                    "image/jpeg",
                    "idempotentVal1",
                    "album123",
                    false,
                    null,
                    Date.from(Instant.ofEpochSecond(1732713392)))),
            Arrays.asList(
                new VideoModel(
                    "foo.mp4",
                    "cachedVal1",
                    "Foo description",
                    "video/mp4",
                    "cachedVal1",
                    "album123",
                    true,
                    Date.from(Instant.ofEpochSecond(1732713392)))));

    List<ImportableData<MediaSerializer.ExportData>> res =
        iterableToList(MediaSerializer.serialize(container));
    assertEquals(3, res.size());

    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"Album\","
            + "  \"id\": \"album123\","
            + "  \"name\": \"Album 123\","
            + "  \"description\": \"Album description\""
            + "}",
        res.get(0).getJsonData());

    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"Video\","
            + "  \"name\": \"foo.mp4\","
            + "  \"description\": \"Foo description\","
            + "  \"albumId\": \"album123\","
            + "  \"uploadedTime\": \"2024-11-27T13:16:32Z\","
            + "  \"favoriteInfo\": {"
            + "    \"@type\": \"FavoriteInfo\","
            + "    \"lastUpdateTime\": \"2024-11-27T13:16:32Z\","
            + "    \"favorite\": false"
            + "  }"
            + "}",
        res.get(1).getJsonData());
    assertTrue(res.get(1) instanceof ImportableFileData);
    assertTrue(((ImportableFileData<?>) res.get(1)).getFile().isInTempStore());
    assertEquals("cachedVal1", ((ImportableFileData<?>) res.get(1)).getFile().getFetchableUrl());

    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"Photo\","
            + "  \"name\": \"bar.jpeg\","
            + "  \"description\": \"Bar description\","
            + "  \"albumId\": \"album123\","
            + "  \"uploadedTime\": \"2024-11-27T13:16:32Z\","
            + "  \"favoriteInfo\": {"
            + "    \"@type\": \"FavoriteInfo\","
            + "    \"lastUpdateTime\": \"2024-11-27T13:16:32Z\","
            + "    \"favorite\": false"
            + "  }"
            + "}",
        res.get(2).getJsonData());
    assertTrue(res.get(2) instanceof ImportableFileData);
    assertFalse(((ImportableFileData<?>) res.get(2)).getFile().isInTempStore());
    assertEquals(
        "https://example.com/bar.jpg",
        ((ImportableFileData<?>) res.get(2)).getFile().getFetchableUrl());
  }
}
