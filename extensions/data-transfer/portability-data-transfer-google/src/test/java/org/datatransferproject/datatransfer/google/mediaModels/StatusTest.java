package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class StatusTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  @Test
  public void status_isSerializable() {
    String statusStringJSON = "{\"code\": 200, \"message\": \"testMessage\"}";

    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      Status status = mapper.readValue(statusStringJSON,
          Status.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(status);
      oos.flush();
      byte[] data = bos.toByteArray();
    } catch (Exception e) {
      serializable = false;
    }
    assertTrue(serializable);
  }
}
