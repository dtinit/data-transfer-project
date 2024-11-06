package org.datatransferproject.transfer.microsoft.photos;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.datatransferproject.transfer.microsoft.StreamChunker;
import org.junit.jupiter.api.Test;

public class StreamChunkerTest {
  private static final int TEST_CHUNK_SIZE = 32000 * 1024; // 32000KiB

  @Test
  public void testSplitDataSingleFullChunk() throws IOException {
    StreamChunker streamChunker =
        new StreamChunker(TEST_CHUNK_SIZE, new ByteArrayInputStream(new byte[TEST_CHUNK_SIZE]));

    Optional<DataChunk> l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getStart()).isEqualTo(0);
    assertThat(l.get().getEnd()).isEqualTo(TEST_CHUNK_SIZE - 1);

    assertThat(streamChunker.nextChunk().isEmpty()).isTrue();
  }

  @Test
  public void testSplitDataSingleNotFullChunk() throws IOException {
    StreamChunker streamChunker =
        new StreamChunker(TEST_CHUNK_SIZE, new ByteArrayInputStream(new byte[TEST_CHUNK_SIZE - 1]));

    Optional<DataChunk> l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE - 1);
    assertThat(l.get().getStart()).isEqualTo(0);
    assertThat(l.get().getEnd()).isEqualTo(TEST_CHUNK_SIZE - 2);

    assertThat(streamChunker.nextChunk().isEmpty()).isTrue();
  }

  @Test
  public void testSplitDataEmpty() throws IOException {
    StreamChunker streamChunker =
        new StreamChunker(TEST_CHUNK_SIZE, new ByteArrayInputStream(new byte[0]));

    Optional<DataChunk> l = streamChunker.nextChunk();
    assertThat(l.isEmpty()).isTrue();
  }

  @Test
  public void testSplitTwoEvenChunks() throws IOException {
    StreamChunker streamChunker =
        new StreamChunker(TEST_CHUNK_SIZE, new ByteArrayInputStream(new byte[TEST_CHUNK_SIZE * 2]));

    Optional<DataChunk> l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getStart()).isEqualTo(0);
    assertThat(l.get().getEnd()).isEqualTo(TEST_CHUNK_SIZE - 1);

    l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getStart()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getEnd()).isEqualTo(2 * TEST_CHUNK_SIZE - 1);

    assertThat(streamChunker.nextChunk().isEmpty()).isTrue();
  }

  @Test
  public void testSplitTwoChunksUneven() throws IOException {
    StreamChunker streamChunker =
        new StreamChunker(
            TEST_CHUNK_SIZE, new ByteArrayInputStream(new byte[TEST_CHUNK_SIZE * 2 - 10]));

    Optional<DataChunk> l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getStart()).isEqualTo(0);
    assertThat(l.get().getEnd()).isEqualTo(TEST_CHUNK_SIZE - 1);

    l = streamChunker.nextChunk();
    assertThat(l.isPresent()).isTrue();
    assertThat(l.get().getSize()).isEqualTo(TEST_CHUNK_SIZE - 10);
    assertThat(l.get().getStart()).isEqualTo(TEST_CHUNK_SIZE);
    assertThat(l.get().getEnd()).isEqualTo(2 * TEST_CHUNK_SIZE - 11);

    assertThat(streamChunker.nextChunk().isEmpty()).isTrue();
  }
}
