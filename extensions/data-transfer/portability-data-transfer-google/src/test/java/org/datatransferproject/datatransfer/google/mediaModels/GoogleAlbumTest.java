package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.Test;
public class GoogleAlbumTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  @Test
  public void googleAlbum_isSerializable() {
    String googleAlbumStringJSON = "{\"id\":\"test_id\", \"title\":\"test_title\", \"isWriteable\":true, \"mediaItemsCount\":10}";
    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      GoogleAlbum googleAlbum = mapper.readValue(googleAlbumStringJSON, GoogleAlbum.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(googleAlbum);
      oos.flush();
      byte [] data = bos.toByteArray();
    } catch (Exception e ) {
      serializable = false;
    }
    assertTrue(serializable);
  }

}
