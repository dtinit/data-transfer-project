package org.datatransferproject.datatransfer.google.mediaModels;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class BatchMediaItemResponseTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  @Test
  public void batchMediaItemResponse_isSerializable() {
    String photoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"focalLength\":\"5.0\", \"apertureFNumber\":\"2.0\", \"isoEquivalent\":\"8.0\", "
        + "\"exposureTime\":\"testExposureTime\"}";
    String videoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"fps\": \"30\", \"status\": \"READY\"}";
    String mediaMetadataStringJSON = format("{\"photo\": %s, \"video\": %s}", photoStringJSON, videoStringJSON);
    String googleMediaItemStringJSON = format("{\"id\":\"test_id\", \"description\":\"test description\","
        + " \"baseUrl\":\"www.testUrl.com\", \"mimeType\":\"image/png\", \"mediaMetadata\": %s,"
        + " \"filename\":\"filename.png\", \"productUrl\":\"www.testProductUrl.com\", "
        + "\"uploadedTime\":\"1697153355456\"}", mediaMetadataStringJSON);

    String statusStringJSON = "{\"code\": 200, \"message\": \"testMessage\"}";
    String newMediaItemStringJSON = format("{\"uploadToken\":\"testUploadToken\", \"status\": %s, "
        + "\"mediaItem\":%s}", statusStringJSON, googleMediaItemStringJSON);

    String batchMediaItemResponseStringJSON = format("{\"newMediaItemResults\":[%s, %s]}", newMediaItemStringJSON, newMediaItemStringJSON);


    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      BatchMediaItemResponse batchMediaItemResponse = mapper.readValue(batchMediaItemResponseStringJSON,
          BatchMediaItemResponse.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(batchMediaItemResponse);
      oos.flush();
      byte[] data = bos.toByteArray();
    } catch (Exception e) {
      serializable = false;
    }
    assertTrue(serializable);
  }

}
