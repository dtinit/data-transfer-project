package org.datatransferproject.datatransfer.google.videos;

import static com.google.common.truth.Truth.assertThat;

import java.io.*;
import org.junit.jupiter.api.Test;

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
    assertThat(readObject).isInstanceOf(VideoResult.class);
    VideoResult deserialized = (VideoResult) readObject;
    assertThat(deserialized.getId()).isEqualTo(videoId);
    assertThat(deserialized.getBytes()).isEqualTo(bytes);
  }
}
