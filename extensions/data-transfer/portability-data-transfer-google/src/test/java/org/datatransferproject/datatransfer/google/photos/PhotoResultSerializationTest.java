package org.datatransferproject.datatransfer.google.photos;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

public class PhotoResultSerializationTest {
  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {
    String photoId = "photoId";
    long bytes = 97397L;
    PhotoResult result = new PhotoResult(photoId, bytes);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ObjectOutputStream(out).writeObject(result);

    ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
    Object readObject = new ObjectInputStream(bais).readObject();
    assertThat(readObject).isInstanceOf(PhotoResult.class);
    PhotoResult deserialized = (PhotoResult) readObject;
    assertThat(deserialized.getId()).isEqualTo(photoId);
    assertThat(deserialized.getBytes()).isEqualTo(bytes);
  }
}
