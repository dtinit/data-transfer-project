package org.datatransferproject.datatransfer.google.videos;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.io.*;
import org.junit.Test;

public class VideoResultSerializationTest {
  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {
    String videoId = "videoId";
    long bytes = 97397L;
    VideoResult result = new VideoResult(videoId, bytes);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ObjectOutputStream(out).writeObject(result);

    ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
    Object readObject = new ObjectInputStream(bais).readObject();
    assertThat(readObject, instanceOf(VideoResult.class));
    VideoResult deserialized = (VideoResult) readObject;
    assertThat(deserialized.getId(), equalTo(videoId));
    assertThat(deserialized.getBytes(), equalTo(bytes));
  }
}
