package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class VideoTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void video_isSerializable() {
    String videoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"fps\": \"30\", \"status\": \"READY\"}";
    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      Video video = mapper.readValue(videoStringJSON, Video.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(videoStringJSON);
      oos.flush();
      byte [] data = bos.toByteArray();
    } catch (Exception e ) {
      serializable = false;
    }
    assertTrue(serializable);
  }
}
