package org.datatransferproject.datatransfer.google.photos;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.io.*;
import org.junit.Test;

public class PhotoResultSerializationTest {
  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {
    String photoId = "photoId";
    long bytes = 97397L;
    MediaResult result = new MediaResult(photoId, bytes);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ObjectOutputStream(out).writeObject(result);

    ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
    Object readObject = new ObjectInputStream(bais).readObject();
    assertThat(readObject, instanceOf(MediaResult.class));
    MediaResult deserialized = (MediaResult) readObject;
    assertThat(deserialized.getId(), equalTo(photoId));
    assertThat(deserialized.getBytes(), equalTo(bytes));
  }
}
