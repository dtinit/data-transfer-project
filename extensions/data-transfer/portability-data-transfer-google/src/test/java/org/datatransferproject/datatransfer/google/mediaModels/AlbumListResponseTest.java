package org.datatransferproject.datatransfer.google.mediaModels;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class AlbumListResponseTest {

  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void albumListResponse_isSerializable() {
    String googleAlbumStringJSON1 = "{\"id\":\"test_id_1\", \"title1\":\"test_title_1\","
        + " \"isWriteable\":true, \"mediaItemsCount\":1}";
    String googleAlbumStringJSON2 = "{\"id\":\"test_id_2\", \"title2\":\"test_title_2\","
        + " \"isWriteable\":false, \"mediaItemsCount\":2}";
    String googleAlbumStringJSON3 = "{\"id\":\"test_id_3\", \"title3\":\"test_title_3\","
        + " \"isWriteable\":true, \"mediaItemsCount\":3}";

    String pagingToken = "TEST_PAGING_TOKEN";
    String googleAlbumListStringJSON = format("[%s, %s, %s]", googleAlbumStringJSON1,
        googleAlbumStringJSON2, googleAlbumStringJSON3);

    String albumListResponseStringJSON = format("{\"nextPageToken\": \"%s\", \"albums\":%s}",
        pagingToken, googleAlbumListStringJSON);

    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      AlbumListResponse albumListResponse = mapper.readValue(albumListResponseStringJSON, AlbumListResponse.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(albumListResponse);
      oos.flush();
      byte [] data = bos.toByteArray();
    } catch (Exception e ) {
      serializable = false;
    }
    assertTrue(serializable);
  }
}
