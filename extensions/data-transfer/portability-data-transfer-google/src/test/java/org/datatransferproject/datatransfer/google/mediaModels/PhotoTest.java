package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class PhotoTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void photo_isSerializable() {
    String photoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"focalLength\":\"5.0\", \"apertureFNumber\":\"2.0\", \"isoEquivalent\":\"8.0\", "
        + "\"exposureTime\":\"testExposureTime\"}";
    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      Photo photo = mapper.readValue(photoStringJSON, Photo.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(photo);
      oos.flush();
      byte [] data = bos.toByteArray();
    } catch (Exception e ) {
      serializable = false;
    }
    assertTrue(serializable);
  }
}
